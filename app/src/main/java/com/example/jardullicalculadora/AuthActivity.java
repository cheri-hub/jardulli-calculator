package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private TextInputEditText editSenha;
    private Button btnEntrar;
    private TextView txtToggle;
    private TextView txtTituloAuth;
    private ProgressBar progressAuth;

    private boolean modoLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        editEmail = findViewById(R.id.editEmail);
        editSenha = findViewById(R.id.editSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        txtToggle = findViewById(R.id.txtToggle);
        txtTituloAuth = findViewById(R.id.txtTituloAuth);
        progressAuth = findViewById(R.id.progressAuth);

        btnEntrar.setOnClickListener(v -> executarAuth());

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

                        // Registra este aparelho como o único autorizado.
                        // Se outra instância logada existir, na próxima sync
                        // dela o device_id não vai bater e ela cairá no logout.
                        SupabaseClient.getInstance().registerDevice(
                                planoManager.getDeviceId(),
                                new SupabaseClient.SupabaseCallback() {
                                    @Override public void onSuccess(JSONObject ignored) {}
                                    @Override public void onError(String err) {
                                        // best-effort: não bloqueia o login
                                    }
                                });

                        // Sincronizar plano
                        planoManager.syncComSupabase(success -> runOnUiThread(() -> {
                            Toast.makeText(AuthActivity.this,
                                    modoLogin ? "Login realizado!" : "Cadastro realizado!",
                                    Toast.LENGTH_SHORT).show();

                            // Redirecionar para PlanosActivity
                            startActivity(new Intent(AuthActivity.this, PlanosActivity.class));
                            finish();
                        }));
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

    private void setLoading(boolean loading) {
        progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!loading);
        editEmail.setEnabled(!loading);
        editSenha.setEnabled(!loading);
    }
}
