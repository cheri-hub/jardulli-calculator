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

/**
 * Recuperação de senha por código de 6 dígitos enviado por e-mail.
 *
 * Passo 1: informa o e-mail e pede o código.
 * Passo 2: informa código + nova senha. A sessão temporária devolvida pelo
 * Supabase fica só em memória e é descartada ao concluir (ou ao sair da tela);
 * o usuário volta ao login e entra com a nova senha.
 */
public class RecuperarSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "EMAIL";

    private TextInputEditText editEmail;
    private TextInputEditText editCodigo;
    private TextInputEditText editNovaSenha;
    private TextInputEditText editConfirmaSenha;
    private View layoutPasso1;
    private View layoutPasso2;
    private TextView txtInfoCodigo;
    private Button btnEnviarCodigo;
    private Button btnRedefinir;
    private ProgressBar progress;

    private static final String ESTADO_EMAIL_ENVIADO = "EMAIL_ENVIADO";

    private TextView txtReenviar;
    private String emailEnviado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);

        editEmail = findViewById(R.id.editEmailRecuperar);
        editCodigo = findViewById(R.id.editCodigo);
        editNovaSenha = findViewById(R.id.editNovaSenha);
        editConfirmaSenha = findViewById(R.id.editConfirmaSenha);
        layoutPasso1 = findViewById(R.id.layoutPasso1);
        layoutPasso2 = findViewById(R.id.layoutPasso2);
        txtInfoCodigo = findViewById(R.id.txtInfoCodigo);
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo);
        btnRedefinir = findViewById(R.id.btnRedefinir);
        progress = findViewById(R.id.progressRecuperar);

        String emailInicial = getIntent().getStringExtra(EXTRA_EMAIL);
        if (emailInicial != null && !emailInicial.isEmpty()) {
            editEmail.setText(emailInicial);
        }

        txtReenviar = findViewById(R.id.txtReenviar);

        btnEnviarCodigo.setOnClickListener(v -> pedirCodigo(texto(editEmail)));
        txtReenviar.setOnClickListener(v -> pedirCodigo(emailEnviado));
        btnRedefinir.setOnClickListener(v -> redefinir());
        findViewById(R.id.btnVoltarLogin).setOnClickListener(v -> finish());

        // Rotação/mudança de configuração: volta direto ao passo 2.
        if (savedInstanceState != null) {
            emailEnviado = savedInstanceState.getString(ESTADO_EMAIL_ENVIADO);
            if (emailEnviado != null) mostrarPasso2();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_EMAIL_ENVIADO, emailEnviado);
    }

    @Override
    protected void onDestroy() {
        // Saiu no meio do fluxo: não deixar a sessão temporária viva em memória.
        // Numa mudança de configuração a tela é recriada e continua o fluxo.
        if (!isChangingConfigurations()) {
            SupabaseClient client = SupabaseClient.getInstance();
            if (client.isSessaoTemporaria()) client.signOut();
        }
        super.onDestroy();
    }

    private void pedirCodigo(String email) {
        String erro = SenhaValidator.validarEmail(email);
        if (erro != null) {
            toast(erro);
            return;
        }
        setLoading(true);
        SupabaseClient.getInstance().recoverPassword(email, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    emailEnviado = email;
                    mostrarPasso2();
                    // Mensagem neutra: não revela se o e-mail tem conta.
                    toast("Se o e-mail estiver cadastrado, enviamos um código.");
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (AuthMensagens.ehLimiteDeEnvio(errorMessage)) {
                        // Já há um código recente: segue para o passo 2 com a mesma
                        // mensagem neutra, sem revelar nada sobre a conta.
                        emailEnviado = email;
                        mostrarPasso2();
                        toast("Se o e-mail estiver cadastrado, enviamos um código.");
                        return;
                    }
                    toast(AuthMensagens.traduzir(errorMessage));
                });
            }
        });
    }

    private void redefinir() {
        String codigo = texto(editCodigo);
        // Trim na senha igual ao login/cadastro (AuthActivity), para não gravar
        // uma senha com espaços que depois o login não reproduz.
        String nova = texto(editNovaSenha);
        String confirmacao = texto(editConfirmaSenha);

        String erro = SenhaValidator.validarCodigo(codigo);
        if (erro == null) erro = SenhaValidator.validarNovaSenha(nova, confirmacao);
        if (erro != null) {
            toast(erro);
            return;
        }

        if (emailEnviado == null) {
            toast("Peça o código primeiro.");
            return;
        }

        setLoading(true);
        SupabaseClient client = SupabaseClient.getInstance();
        if (client.isSessaoTemporaria()) {
            // Código já validado numa tentativa anterior (ex.: falha ao trocar a senha).
            atualizarSenha(nova);
            return;
        }
        client.verifyRecovery(emailEnviado, codigo,
                new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        // A tela pode ter sido fechada enquanto o código era validado.
                        if (isDestroyed() && !isChangingConfigurations()) {
                            client.signOut();
                            return;
                        }
                        atualizarSenha(nova);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            toast(AuthMensagens.traduzir(errorMessage));
                        });
                    }
                });
    }

    private void atualizarSenha(String nova) {
        SupabaseClient.getInstance().updatePassword(nova, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    SupabaseClient.getInstance().signOut();
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_EMAIL, emailEnviado));
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    toast(AuthMensagens.traduzir(errorMessage));
                });
            }
        });
    }

    private void mostrarPasso2() {
        layoutPasso1.setVisibility(View.GONE);
        layoutPasso2.setVisibility(View.VISIBLE);
        txtInfoCodigo.setText("Enviamos um código para " + emailEnviado
                + ". Digite-o abaixo e escolha a nova senha.");
        editCodigo.requestFocus();
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }

    private void toast(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEnviarCodigo.setEnabled(!loading);
        btnRedefinir.setEnabled(!loading);
        txtReenviar.setEnabled(!loading);
    }
}
