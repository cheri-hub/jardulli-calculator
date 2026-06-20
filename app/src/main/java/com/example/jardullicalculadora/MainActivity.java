package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PlanoManager planoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        planoManager = new PlanoManager(this);
        initNavigation();
        atualizarBadgePlano();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarBadgePlano();
        planoManager.syncComSupabase(success -> runOnUiThread(this::atualizarBadgePlano));
    }

    private void initNavigation() {
        findViewById(R.id.cardNovoBalanco).setOnClickListener(v -> {
            if (planoManager.podeFazerBalanco()) {
                startActivity(new Intent(this, TratorOperacaoActivity.class));
            } else {
                mostrarDialogLimite();
            }
        });

        findViewById(R.id.cardPatinagem).setOnClickListener(v ->
                startActivity(new Intent(this, AvancoRatioActivity.class)));

        findViewById(R.id.cardHistorico).setOnClickListener(v -> {
            if (planoManager.podeVerHistorico()) {
                startActivity(new Intent(this, HistoricoActivity.class));
            } else {
                mostrarDialogUpsell("Histórico",
                        "O histórico de balanços está disponível a partir do plano Flexível.");
            }
        });

        findViewById(R.id.cardMeuPlano).setOnClickListener(v -> {
            if (SupabaseClient.getInstance().isLoggedIn()) {
                startActivity(new Intent(this, PlanosActivity.class));
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
        });

        findViewById(R.id.cardSuporte).setOnClickListener(v ->
                startActivity(new Intent(this, SuporteActivity.class)));

        findViewById(R.id.cardMinhaConta).setOnClickListener(v -> {
            if (SupabaseClient.getInstance().isLoggedIn()) {
                startActivity(new Intent(this, PerfilActivity.class));
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
        });
    }

    private void atualizarBadgePlano() {
        TextView txtPlano = findViewById(R.id.txtPlanoAtual);
        Plano plano = planoManager.getPlanoAtual();
        txtPlano.setText(plano.getId().toUpperCase(Locale.ROOT));

        TextView txtBalancos = findViewById(R.id.txtBalancosRestantes);
        if (plano == Plano.TESTDRIVE) {
            int restantes = plano.getLimiteBalancosMes() - planoManager.getBalancosMesAtual();
            txtBalancos.setText("Restam " + Math.max(0, restantes) + " balanço(s) este mês");
        } else {
            txtBalancos.setText("Calcular distribuição de peso");
        }

        // Atualiza subtítulo do card "Minha Conta" conforme login.
        TextView txtConta = findViewById(R.id.txtMinhaContaSubtitulo);
        if (txtConta != null) {
            txtConta.setText(SupabaseClient.getInstance().isLoggedIn()
                    ? "Editar perfil ou sair"
                    : "Entrar ou cadastrar");
        }
    }

    private void mostrarDialogLimite() {
        new AlertDialog.Builder(this)
                .setTitle("Limite Atingido")
                .setMessage("Você atingiu o limite de balanços do plano Testdrive (1/mês). Faça upgrade para continuar.")
                .setPositiveButton("Ver Planos", (d, w) -> {
                    if (SupabaseClient.getInstance().isLoggedIn()) {
                        startActivity(new Intent(this, PlanosActivity.class));
                    } else {
                        startActivity(new Intent(this, AuthActivity.class));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogUpsell(String feature, String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(feature + " Bloqueado")
                .setMessage(mensagem)
                .setPositiveButton("Ver Planos", (d, w) -> {
                    if (SupabaseClient.getInstance().isLoggedIn()) {
                        startActivity(new Intent(this, PlanosActivity.class));
                    } else {
                        startActivity(new Intent(this, AuthActivity.class));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
