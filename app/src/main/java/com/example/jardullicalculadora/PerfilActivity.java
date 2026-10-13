package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class PerfilActivity extends AppCompatActivity {

    /** Quando true, exibe banner explicando que o perfil é obrigatório p/ assinar. */
    public static final String EXTRA_REQUIRED_FOR_SUBSCRIPTION = "required_for_subscription";

    private TextInputEditText editNome;
    private TextInputEditText editCpfCnpj;
    private TextInputEditText editTelefone;
    private ProgressBar progressPerfil;
    private Button btnSalvar;
    private Button btnSair;
    private Button btnExcluirConta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        editNome = findViewById(R.id.editNome);
        editCpfCnpj = findViewById(R.id.editCpfCnpj);
        editTelefone = findViewById(R.id.editTelefone);
        progressPerfil = findViewById(R.id.progressPerfil);
        btnSalvar = findViewById(R.id.btnSalvar);

        btnSalvar.setOnClickListener(v -> salvarPerfil());

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        btnSair = findViewById(R.id.btnSair);
        btnSair.setOnClickListener(v -> confirmarLogout());

        btnExcluirConta = findViewById(R.id.btnExcluirConta);
        btnExcluirConta.setOnClickListener(v -> confirmarExclusao());

        if (getIntent().getBooleanExtra(EXTRA_REQUIRED_FOR_SUBSCRIPTION, false)) {
            Toast.makeText(this,
                    "Complete seus dados antes de assinar um plano (exigência do pagamento).",
                    Toast.LENGTH_LONG).show();
        }

        carregarPerfil();
    }

    private void carregarPerfil() {
        if (!SupabaseClient.getInstance().isLoggedIn()) return;
        setLoading(true);
        SupabaseClient.getInstance().fetchProfile(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    JSONArray data = response.optJSONArray("data");
                    if (data == null || data.length() == 0) return;
                    JSONObject p = data.optJSONObject(0);
                    if (p == null) return;
                    editNome.setText(p.optString("full_name", ""));
                    editCpfCnpj.setText(p.optString("cpf_cnpj", ""));
                    editTelefone.setText(p.optString("phone", ""));
                });
            }
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> setLoading(false));
            }
        });
    }

    private void salvarPerfil() {
        String nome = editNome.getText() != null ? editNome.getText().toString().trim() : "";
        String cpfCnpj = editCpfCnpj.getText() != null ? editCpfCnpj.getText().toString().trim() : "";
        String telefone = editTelefone.getText() != null ? editTelefone.getText().toString().trim() : "";

        if (nome.isEmpty()) {
            Toast.makeText(this, "Preencha o nome", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cpfCnpj.isEmpty()) {
            Toast.makeText(this, "Preencha o CPF ou CNPJ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!DocumentValidator.isValid(cpfCnpj)) {
            Toast.makeText(this, "CPF ou CNPJ inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (telefone.isEmpty()) {
            Toast.makeText(this, "Preencha o telefone", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        SupabaseClient.getInstance().updateProfile(nome, cpfCnpj, telefone,
                new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(PerfilActivity.this, "Perfil salvo!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(PerfilActivity.this, "Erro: " + errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private boolean validarCpfCnpj(String doc) {
        return DocumentValidator.isValid(doc);
    }

    /** @deprecated use {@link DocumentValidator#isValidCpf(String)} */
    @Deprecated
    private boolean validarCpf(String cpf) {
        return DocumentValidator.isValidCpf(cpf);
    }

    /** @deprecated use {@link DocumentValidator#isValidCnpj(String)} */
    @Deprecated
    private boolean validarCnpj(String cnpj) {
        return DocumentValidator.isValidCnpj(cnpj);
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da Conta")
                .setMessage("Tem certeza que deseja sair? Você precisará entrar novamente para acessar seu plano e histórico.")
                .setPositiveButton("Sair", (d, w) -> {
                    PlanoManager pm = new PlanoManager(this);
                    pm.forcarLogout();
                    Toast.makeText(this, "Você saiu da conta", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarExclusao() {
        EditText campo = new EditText(this);
        campo.setHint("Digite " + ExclusaoConta.PALAVRA_DE_CONFIRMACAO);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        FrameLayout container = new FrameLayout(this);
        int margem = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(margem, 0, margem, 0);
        container.addView(campo);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Excluir conta")
                .setMessage("Isso apaga definitivamente seu login, seu perfil, seu histórico na nuvem "
                        + "e sua assinatura. Não dá para desfazer.\n\n"
                        + "Se você tem assinatura ativa, ela será cancelada agora e o acesso ao plano "
                        + "termina na hora, sem reembolso proporcional. Para pedir o reembolso dos "
                        + "7 dias, fale com o suporte antes de excluir.\n\n"
                        + "Os balanços salvos neste aparelho não são apagados.\n\n"
                        + "Para confirmar, digite " + ExclusaoConta.PALAVRA_DE_CONFIRMACAO + ":")
                .setView(container)
                .setPositiveButton("Excluir definitivamente", (d, w) -> excluirConta())
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button confirmar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmar.setEnabled(false);
            confirmar.setTextColor(0xFFC62828);
            campo.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    confirmar.setEnabled(ExclusaoConta.confirmacaoValida(s.toString()));
                }
            });
        });
        dialog.show();
    }

    private void excluirConta() {
        setLoading(true);
        SupabaseClient.getInstance().deleteAccount(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    // A conta já não existe no servidor: descarta a sessão e o plano em cache.
                    new PlanoManager(PerfilActivity.this).forcarLogout();
                    Toast.makeText(PerfilActivity.this, "Conta excluída.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(PerfilActivity.this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(PerfilActivity.this,
                            ExclusaoConta.mensagemDeErro(errorMessage), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressPerfil.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSair.setEnabled(!loading);
        btnExcluirConta.setEnabled(!loading);
        btnSalvar.setEnabled(!loading);
        editNome.setEnabled(!loading);
        editCpfCnpj.setEnabled(!loading);
        editTelefone.setEnabled(!loading);
    }
}
