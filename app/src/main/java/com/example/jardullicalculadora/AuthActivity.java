package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private TextInputEditText editSenha;
    private Button btnEntrar;
    private TextView txtToggle;
    private TextView txtTituloAuth;
    private TextView txtEsqueciSenha;
    private ProgressBar progressAuth;

    private boolean modoLogin = true;

    /** Volta da recuperação de senha: preenche o e-mail e pede para entrar com a nova senha. */
    private final ActivityResultLauncher<Intent> recuperarSenhaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null) return;
                String email = data.getStringExtra(RecuperarSenhaActivity.EXTRA_EMAIL);
                if (email != null) editEmail.setText(email);
                editSenha.setText("");
                modoLogin = true;
                atualizarUI();
                Toast.makeText(this, "Senha redefinida. Entre com a nova senha.",
                        Toast.LENGTH_LONG).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        txtToggle = findViewById(R.id.txtToggle);
        txtTituloAuth = findViewById(R.id.txtTituloAuth);
        txtEsqueciSenha = findViewById(R.id.txtEsqueciSenha);
        progressAuth = findViewById(R.id.progressAuth);

        btnEntrar.setOnClickListener(v -> executarAuth());

        txtEsqueciSenha.setOnClickListener(v -> {
            String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
            recuperarSenhaLauncher.launch(new Intent(this, RecuperarSenhaActivity.class)
                    .putExtra(RecuperarSenhaActivity.EXTRA_EMAIL, email));
        });

        txtToggle.setOnClickListener(v -> {
            modoLogin = !modoLogin;
            atualizarUI();
        });

        Button btnSemConta = findViewById(R.id.btnSemConta);
        btnSemConta.setOnClickListener(v -> {
            finish();
        });

        atualizarUI();
    }

    private void atualizarUI() {
        txtEsqueciSenha.setVisibility(modoLogin ? View.VISIBLE : View.GONE);
        if (modoLogin) {
            txtTituloAuth.setText("Entrar");
            btnEntrar.setText("Entrar");
            txtToggle.setText("Não tem conta? Cadastre-se");
        } else {
            txtTituloAuth.setText("Cadastrar");
            btnEntrar.setText("Cadastrar");
            txtToggle.setText("Já tem conta? Entre");
        }
    }

    private void executarAuth() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String senha = editSenha.getText() != null ? editSenha.getText().toString().trim() : "";

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        if (senha.length() < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        SupabaseClient client = SupabaseClient.getInstance();
        SupabaseClient.SupabaseCallback callback = new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    setLoading(false);

                    // Salvar sessão
                    String accessToken = response.optString("access_token", null);
                    String refreshToken = response.optString("refresh_token", null);
                    JSONObject user = response.optJSONObject("user");
                    String userId = user != null ? user.optString("id", null) : null;

                    if (accessToken != null && userId != null) {
                        PlanoManager planoManager = new PlanoManager(AuthActivity.this);
                        planoManager.salvarSessao(accessToken, refreshToken, userId);

                        // Se a conta já estiver ativa em outro aparelho, pergunta
                        // antes de transferir o acesso (sobrescrever o device id).
                        concluirLoginComChecagemDeDispositivo(planoManager);
                    } else {
                        Toast.makeText(AuthActivity.this,
                                modoLogin ? "Login realizado!" : "Verifique seu email para confirmar o cadastro",
                                Toast.LENGTH_LONG).show();
                        if (modoLogin) {
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        };

        if (modoLogin) {
            client.signIn(email, senha, callback);
        } else {
            client.signUp(email, senha, callback);
        }
    }

    /**
     * Após salvar a sessão, verifica se a conta já está registrada em OUTRO
     * aparelho. Se estiver, pergunta se o usuário quer transferir o acesso para
     * este aparelho antes de sobrescrever o {@code current_device_id}. Sem outro
     * aparelho (ou se a checagem falhar por rede), prossegue direto.
     */
    private void concluirLoginComChecagemDeDispositivo(PlanoManager planoManager) {
        String local = planoManager.getDeviceId();
        SupabaseClient.getInstance().fetchCurrentDeviceId(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    String remoto = null;
                    JSONArray data = response.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        JSONObject row = data.optJSONObject(0);
                        if (row != null && !row.isNull("current_device_id")) {
                            remoto = row.optString("current_device_id", null);
                        }
                    }
                    boolean outroAparelho = remoto != null && !remoto.isEmpty()
                            && !"null".equals(remoto) && !remoto.equals(local);
                    if (outroAparelho) {
                        perguntarTransferencia(planoManager);
                    } else {
                        prosseguirRegistrandoDispositivo(planoManager);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Sem conseguir checar (ex.: offline), mantém o comportamento atual.
                runOnUiThread(() -> prosseguirRegistrandoDispositivo(planoManager));
            }
        });
    }

    private void perguntarTransferencia(PlanoManager planoManager) {
        new AlertDialog.Builder(this)
                .setTitle("Conta ativa em outro aparelho")
                .setMessage("Sua conta já está ativa em outro aparelho. Deseja transferir o "
                        + "acesso para este aparelho? O outro aparelho será desconectado.")
                .setCancelable(false)
                .setPositiveButton("Transferir", (d, w) ->
                        prosseguirRegistrandoDispositivo(planoManager))
                .setNegativeButton("Cancelar", (d, w) -> {
                    // Não transfere: encerra a sessão neste aparelho; o acesso
                    // permanece no aparelho antigo.
                    planoManager.forcarLogout();
                    Toast.makeText(this, "Acesso mantido no outro aparelho.",
                            Toast.LENGTH_LONG).show();
                })
                .show();
    }

    /**
     * Registra este aparelho como o único autorizado (sobrescreve o anterior,
     * que cairá no logout na próxima sync), sincroniza o plano e segue para
     * a tela de planos.
     */
    private void prosseguirRegistrandoDispositivo(PlanoManager planoManager) {
        SupabaseClient.getInstance().registerDevice(
                planoManager.getDeviceId(),
                new SupabaseClient.SupabaseCallback() {
                    @Override public void onSuccess(JSONObject ignored) {}
                    @Override public void onError(String err) {
                        // best-effort: não bloqueia o login
                    }
                });

        planoManager.syncComSupabase(success -> runOnUiThread(() -> {
            Toast.makeText(AuthActivity.this,
                    modoLogin ? "Login realizado!" : "Cadastro realizado!",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(AuthActivity.this, PlanosActivity.class));
            finish();
        }));
    }

    private void setLoading(boolean loading) {
        progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!loading);
        editEmail.setEnabled(!loading);
        editSenha.setEnabled(!loading);
    }
}
