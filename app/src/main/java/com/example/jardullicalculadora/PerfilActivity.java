package com.example.jardullicalculadora;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        Button btnSair = findViewById(R.id.btnSair);
        btnSair.setOnClickListener(v -> confirmarLogout());

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

    private void setLoading(boolean loading) {
        progressPerfil.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!loading);
        editNome.setEnabled(!loading);
        editCpfCnpj.setEnabled(!loading);
        editTelefone.setEnabled(!loading);
    }
}
