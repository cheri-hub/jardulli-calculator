package com.example.jardullicalculadora;

import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoricoActivity extends AppCompatActivity {

    private static final String TAG = "HistoricoActivity";

    private RecyclerView recyclerView;
    private HistoricoAdapter adapter;
    private TextView txtVazio;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        dbHelper = new DBHelper(this);
        recyclerView = findViewById(R.id.recyclerHistorico);
        txtVazio = findViewById(R.id.txtVazio);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        carregarHistorico();
    }

    /**
     * Carrega historíco local primeiro (resposta imediata) e, se o usuário
     * estiver logado, busca no Supabase e mescla por {@code data_criacao}
     * para evitar duplicatas.
     */
    private void carregarHistorico() {
        List<HistoricoAdapter.BalancoItem> locais = lerLocais();
        renderizar(locais);

        SupabaseClient client = SupabaseClient.getInstance();
        if (!client.isLoggedIn()) return;

        client.fetchHistorico(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    List<HistoricoAdapter.BalancoItem> remotos = parseRemotos(response);
                    List<HistoricoAdapter.BalancoItem> mesclados = mesclar(locais, remotos);
                    renderizar(mesclados);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Historíco em nuvem indisponível: " + errorMessage);
            }
        });
    }

    private List<HistoricoAdapter.BalancoItem> lerLocais() {
        List<ContentValues> balancos = dbHelper.listarBalancos();
        List<HistoricoAdapter.BalancoItem> items = new ArrayList<>();
        for (ContentValues cv : balancos) {
            HistoricoAdapter.BalancoItem item = new HistoricoAdapter.BalancoItem();
            item.id = cv.getAsLong("id");
            item.data = cv.getAsString("data_criacao");
            item.serie = cv.getAsString("serie_trator");
            item.modelo = cv.getAsString("modelo");
            item.operacao = cv.getAsString("operacao");
            item.pesoTotal = cv.getAsDouble("peso_total");
            items.add(item);
        }
        return items;
    }

    private List<HistoricoAdapter.BalancoItem> parseRemotos(JSONObject response) {
        List<HistoricoAdapter.BalancoItem> items = new ArrayList<>();
        JSONArray arr = response.optJSONArray("data");
        if (arr == null) return items;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null) continue;
            HistoricoAdapter.BalancoItem item = new HistoricoAdapter.BalancoItem();
            item.remoteId = row.optString("id", null);
            item.data = row.optString("data_criacao", "");
            item.serie = row.optString("serie_trator", "");
            item.modelo = row.optString("modelo", "");
            item.operacao = row.optString("operacao", "");
            item.pesoTotal = row.optDouble("peso_total", 0d);
            items.add(item);
        }
        return items;
    }

    /**
     * Une listas local e remota deduplicando por {@code data_criacao + modelo + peso_total}.
     * Prioriza a versão local (que tem id long para deletar offline).
     */
    private List<HistoricoAdapter.BalancoItem> mesclar(
            List<HistoricoAdapter.BalancoItem> locais,
            List<HistoricoAdapter.BalancoItem> remotos) {
        List<HistoricoAdapter.BalancoItem> resultado = new ArrayList<>(locais);
        Set<String> chavesLocais = new HashSet<>();
        for (HistoricoAdapter.BalancoItem it : locais) {
            chavesLocais.add(chaveDedup(it));
        }
        for (HistoricoAdapter.BalancoItem it : remotos) {
            if (!chavesLocais.contains(chaveDedup(it))) {
                resultado.add(it);
            }
        }
        // ordena por data desc (string ISO-8601 ord. lexicográfica = ord. cronológica)
        resultado.sort((a, b) -> {
            String da = a.data == null ? "" : a.data;
            String db = b.data == null ? "" : b.data;
            return db.compareTo(da);
        });
        return resultado;
    }

    private String chaveDedup(HistoricoAdapter.BalancoItem it) {
        // truncar segundos: cloud e local podem ter timezones diferentes na borda;
        // usamos minuto + modelo + peso (arredondado) como heurística estável.
        String d = it.data == null ? "" : (it.data.length() >= 16 ? it.data.substring(0, 16) : it.data);
        return d + "|" + it.modelo + "|" + Math.round(it.pesoTotal);
    }

    private void renderizar(List<HistoricoAdapter.BalancoItem> items) {
        if (items.isEmpty()) {
            txtVazio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtVazio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter = new HistoricoAdapter(items, this::confirmarDeletar);
        recyclerView.setAdapter(adapter);
    }

    private void confirmarDeletar(HistoricoAdapter.BalancoItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Deletar Balanço")
                .setMessage("Deseja realmente excluir este balanço de " + item.modelo + "?")
                .setPositiveButton("Excluir", (d, w) -> deletarBalanco(item))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarBalanco(HistoricoAdapter.BalancoItem item) {
        boolean removidoLocal = false;
        if (item.id > 0) {
            removidoLocal = dbHelper.deletarBalanco(item.id);
        }

        SupabaseClient client = SupabaseClient.getInstance();
        if (item.remoteId != null && client.isLoggedIn()) {
            client.deleteBalanco(item.remoteId, new SupabaseClient.SupabaseCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        Toast.makeText(HistoricoActivity.this, "Balanço excluído", Toast.LENGTH_SHORT).show();
                        carregarHistorico();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        Log.w(TAG, "Falha ao excluir balanço remoto: " + errorMessage);
                        Toast.makeText(HistoricoActivity.this,
                                "Removido localmente; falha na nuvem", Toast.LENGTH_LONG).show();
                        carregarHistorico();
                    });
                }
            });
        } else {
            if (removidoLocal) {
                Toast.makeText(this, "Balanço excluído", Toast.LENGTH_SHORT).show();
            }
            carregarHistorico();
        }
    }
}
