package com.example.jardullicalculadora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class PlanosActivity extends AppCompatActivity {

    private static final String TAG = "PlanosActivity";

    private LinearLayout layoutPlanos;
    private ProgressBar progressPlanos;
    private PlanoManager planoManager;
    private Handler handler;
    private boolean polling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planos);

        planoManager = new PlanoManager(this);
        handler = new Handler(Looper.getMainLooper());

        layoutPlanos = findViewById(R.id.layoutPlanos);
        progressPlanos = findViewById(R.id.progressPlanos);

        TextView txtPlanoAtual = findViewById(R.id.txtPlanoAtualHeader);
        txtPlanoAtual.setText("Plano atual: " + planoManager.getPlanoAtual().getId().toUpperCase(Locale.ROOT));

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        Button btnPerfil = findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class)));

        exibirPlanos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (polling) {
            iniciarPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pararPolling();
    }

    private void exibirPlanos() {
        layoutPlanos.removeAllViews();

        for (Plano plano : Plano.values()) {
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_plano, layoutPlanos, false);

            TextView txtNome = itemView.findViewById(R.id.txtNomePlano);
            TextView txtPreco = itemView.findViewById(R.id.txtPreco);
            TextView txtFeatures = itemView.findViewById(R.id.txtFeatures);
            TextView txtBadge = itemView.findViewById(R.id.txtBadgeAtual);
            Button btnAssinar = itemView.findViewById(R.id.btnAssinar);

            txtNome.setText(plano.getId().toUpperCase(Locale.ROOT));

            switch (plano) {
                case TESTDRIVE:
                    txtPreco.setText("Grátis");
                    txtFeatures.setText("✅ 1 balanço/mês\n✅ Lastro\n✅ Patinagem\n❌ PDF\n❌ Histórico\n📖 FAQ");
                    break;
                case FLEXIVEL:
                    txtPreco.setText("R$ 30/mês");
                    txtFeatures.setText("✅ Balanços ilimitados\n✅ Lastro\n✅ Patinagem\n❌ PDF\n✅ Histórico\n📧 Email");
                    break;
                case PROFISSIONAL:
                    txtPreco.setText("R$ 150/semestre (R$ 25/mês)");
                    txtFeatures.setText("✅ Balanços ilimitados\n✅ Lastro\n✅ Patinagem\n✅ PDF\n✅ Histórico\n💬 WhatsApp");
                    break;
                case FROTISTA:
                    txtPreco.setText("R$ 240/ano (R$ 20/mês)");
                    txtFeatures.setText("✅ Balanços ilimitados\n✅ Lastro\n✅ Patinagem\n✅ PDF\n✅ Histórico\n⭐ Prioritário");
                    break;
            }

            boolean isAtual = planoManager.getPlanoAtual() == plano;
            if (isAtual) {
                txtBadge.setVisibility(View.VISIBLE);
                if (plano != Plano.TESTDRIVE && planoManager.getSubscriptionId() != null) {
                    btnAssinar.setText("Cancelar Plano");
                    btnAssinar.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFD32F2F));
                    btnAssinar.setOnClickListener(v -> confirmarCancelamento());
                } else {
                    btnAssinar.setText("Plano Atual");
                    btnAssinar.setEnabled(false);
                    btnAssinar.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF9E9E9E));
                }
            } else if (plano == Plano.TESTDRIVE) {
                btnAssinar.setVisibility(View.GONE);
            } else {
                btnAssinar.setOnClickListener(v -> assinarPlano(plano));
            }

            layoutPlanos.addView(itemView);
        }
    }

    private void assinarPlano(Plano plano) {
        SupabaseClient client = SupabaseClient.getInstance();
        if (!client.isLoggedIn()) {
            Toast.makeText(this, "Faça login para assinar", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
            return;
        }

        progressPlanos.setVisibility(View.VISIBLE);

        // Pré-checagem: o Asaas exige nome, CPF/CNPJ e telefone do cliente.
        // Sem esses dados a Edge function `subscribe` retorna 422 profile_incomplete.
        client.fetchProfile(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                org.json.JSONArray arr = response.optJSONArray("data");
                JSONObject p = (arr != null && arr.length() > 0) ? arr.optJSONObject(0) : null;
                String fullName = p != null ? p.optString("full_name", "").trim() : "";
                String cpfCnpj  = p != null ? p.optString("cpf_cnpj", "").trim()  : "";
                String phone    = p != null ? p.optString("phone", "").trim()     : "";

                boolean perfilCompleto = !fullName.isEmpty()
                        && !phone.isEmpty()
                        && DocumentValidator.isValid(cpfCnpj);

                runOnUiThread(() -> {
                    if (!perfilCompleto) {
                        progressPlanos.setVisibility(View.GONE);
                        Toast.makeText(PlanosActivity.this,
                                "Complete seu perfil antes de assinar.",
                                Toast.LENGTH_LONG).show();
                        Intent i = new Intent(PlanosActivity.this, PerfilActivity.class);
                        i.putExtra(PerfilActivity.EXTRA_REQUIRED_FOR_SUBSCRIPTION, true);
                        startActivity(i);
                        return;
                    }
                    enviarSubscribe(plano);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressPlanos.setVisibility(View.GONE);
                    Toast.makeText(PlanosActivity.this,
                            "Não foi possível verificar seu perfil: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void enviarSubscribe(Plano plano) {
        SupabaseClient client = SupabaseClient.getInstance();
        progressPlanos.setVisibility(View.VISIBLE);

        JSONObject body = new JSONObject();
        try {
            body.put("plan_id", plano.getId());
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao montar body", e);
            return;
        }

        client.callEdgeFunction("subscribe", body, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    progressPlanos.setVisibility(View.GONE);
                    // Edge functions retornam {"data":{...},"error":null}
                    JSONObject data = response.optJSONObject("data");
                    String checkoutUrl = data != null
                            ? data.optString("checkout_url", null)
                            : response.optString("checkout_url", null);
                    if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                        startActivity(intent);
                        polling = true;
                    } else {
                        Log.w(TAG, "subscribe sem checkout_url. response=" + response);
                        Toast.makeText(PlanosActivity.this,
                                "Erro ao obter URL de pagamento", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressPlanos.setVisibility(View.GONE);
                    if (errorMessage != null && errorMessage.toLowerCase().contains("profile_incomplete")) {
                        Toast.makeText(PlanosActivity.this,
                                "Complete seu perfil antes de assinar.", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(PlanosActivity.this, PerfilActivity.class);
                        i.putExtra(PerfilActivity.EXTRA_REQUIRED_FOR_SUBSCRIPTION, true);
                        startActivity(i);
                    } else {
                        Toast.makeText(PlanosActivity.this,
                                "Erro: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void iniciarPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!polling) return;

                planoManager.syncComSupabase(success -> runOnUiThread(() -> {
                    if (success && planoManager.getPlanoAtual() != Plano.TESTDRIVE) {
                        polling = false;
                        Toast.makeText(PlanosActivity.this,
                                "Plano atualizado: " + planoManager.getPlanoAtual().getId().toUpperCase(Locale.ROOT),
                                Toast.LENGTH_LONG).show();
                        exibirPlanos();
                        TextView txtPlanoAtual = findViewById(R.id.txtPlanoAtualHeader);
                        txtPlanoAtual.setText("Plano atual: " + planoManager.getPlanoAtual().getId().toUpperCase(Locale.ROOT));
                    } else if (polling) {
                        handler.postDelayed(this, 3000);
                    }
                }));
            }
        }, 3000);
    }

    private void pararPolling() {
        handler.removeCallbacksAndMessages(null);
    }

    private void confirmarCancelamento() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancelar Assinatura")
                .setMessage("Sua assinatura será cancelada e o acesso continuará até o fim do período já pago. Deseja prosseguir?")
                .setPositiveButton("Cancelar Assinatura", (d, w) -> cancelarAssinatura())
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void cancelarAssinatura() {
        String subId = planoManager.getSubscriptionId();
        if (subId == null) {
            Toast.makeText(this, "Nenhuma assinatura ativa encontrada", Toast.LENGTH_SHORT).show();
            return;
        }
        progressPlanos.setVisibility(View.VISIBLE);
        SupabaseClient.getInstance().cancelSubscription(subId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    progressPlanos.setVisibility(View.GONE);
                    String msg = response.optString("message", "Assinatura cancelada");
                    Toast.makeText(PlanosActivity.this, msg, Toast.LENGTH_LONG).show();
                    // Re-sync para refletir status no app
                    planoManager.syncComSupabase(s -> runOnUiThread(() -> {
                        exibirPlanos();
                        TextView txtPlanoAtual = findViewById(R.id.txtPlanoAtualHeader);
                        txtPlanoAtual.setText("Plano atual: " + planoManager.getPlanoAtual().getId().toUpperCase(Locale.ROOT));
                    }));
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressPlanos.setVisibility(View.GONE);
                    Toast.makeText(PlanosActivity.this, "Erro ao cancelar: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
