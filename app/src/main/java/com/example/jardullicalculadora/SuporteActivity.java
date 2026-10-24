package com.example.jardullicalculadora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SuporteActivity extends AppCompatActivity {

    private static final String EMAIL_SUPORTE = "suporte@jardulli.com.br";
    // Número de WhatsApp configurado em local.properties (WHATSAPP_SUPORTE).
    // Vazio => botão de WhatsApp fica oculto. Evita expor placeholder em produção.
    private static final String WHATSAPP_NUMERO = BuildConfig.WHATSAPP_SUPORTE;

    private static final String[][] FAQ_ITEMS = {
            {"O que é a Calculadora Jardulli?",
                    "É uma ferramenta para parametrização de tratores agrícolas, permitindo calcular a distribuição de pesos, lastro líquido, rodados e gerar relatórios completos."},
            {"Como calcular a distribuição de peso?",
                    "Acesse 'Novo Balanço' na tela principal, selecione a série e modelo do trator, configure rodados, lastro líquido e pesos frontais/traseiros. O relatório será gerado automaticamente."},
            {"O que é patinagem?",
                    "Patinagem é o deslizamento percentual das rodas do trator em relação ao solo. A faixa ideal é de 0% a 8%. Valores acima indicam necessidade de ajuste no lastro."},
            {"Como gerar o relatório em PDF?",
                    "O relatório PDF está disponível nos planos pagos (Flexível, Profissional e Frotista). Ao concluir um balanço, clique em 'Gerar PDF' na tela de resultados."},
            {"Posso usar offline?",
                    "Sim! Os cálculos funcionam offline. Apenas login, sincronização de planos e pagamentos precisam de conexão à internet."},
            {"Como mudar de plano?",
                    "Acesse 'Meu Plano' na tela principal para ver as opções disponíveis e realizar upgrade ou cancelamento."},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suporte);

        PlanoManager planoManager = new PlanoManager(this);
        String tipoSuporte = planoManager.getTipoSuporte();
        Plano plano = planoManager.getPlanoAtual();

        TextView txtTipoSuporte = findViewById(R.id.txtTipoSuporte);
        txtTipoSuporte.setText("Plano: " + plano.getId().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + plano.getId().substring(1));

        configurarCanais(tipoSuporte);
        configurarFAQ();

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void configurarCanais(String tipoSuporte) {
        Button btnEmail = findViewById(R.id.btnEmail);
        Button btnWhatsApp = findViewById(R.id.btnWhatsApp);
        TextView txtPrioritario = findViewById(R.id.txtPrioritario);

        switch (tipoSuporte) {
            case "PRIORITARIO":
                txtPrioritario.setVisibility(View.VISIBLE);
                // fall through
            case "WHATSAPP":
                if (WHATSAPP_NUMERO != null && !WHATSAPP_NUMERO.isEmpty()) {
                    btnWhatsApp.setVisibility(View.VISIBLE);
                    btnWhatsApp.setOnClickListener(v -> abrirWhatsApp());
                }
                // fall through
            case "EMAIL":
                btnEmail.setVisibility(View.VISIBLE);
                btnEmail.setOnClickListener(v -> abrirEmail());
                break;
            default: // FAQ only
                break;
        }
    }

    private void configurarFAQ() {
        LinearLayout layoutFAQ = findViewById(R.id.layoutFAQ);

        for (String[] faq : FAQ_ITEMS) {
            TextView txtPergunta = new TextView(this);
            txtPergunta.setText("❓ " + faq[0]);
            txtPergunta.setTextSize(14);
            txtPergunta.setTextColor(0xFF333333);
            txtPergunta.setPadding(0, 16, 0, 4);
            txtPergunta.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView txtResposta = new TextView(this);
            txtResposta.setText(faq[1]);
            txtResposta.setTextSize(13);
            txtResposta.setTextColor(0xFF757575);
            txtResposta.setPadding(0, 0, 0, 8);
            txtResposta.setVisibility(View.GONE);

            txtPergunta.setOnClickListener(v -> {
                if (txtResposta.getVisibility() == View.GONE) {
                    txtResposta.setVisibility(View.VISIBLE);
                } else {
                    txtResposta.setVisibility(View.GONE);
                }
            });

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFE0E0E0);

            layoutFAQ.addView(txtPergunta);
            layoutFAQ.addView(txtResposta);
            layoutFAQ.addView(divider);
        }
    }

    private void abrirEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + EMAIL_SUPORTE));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Suporte - Jardulli Calculadora");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w("SuporteActivity", "Nenhum cliente de email instalado", e);
            Toast.makeText(this,
                    "Nenhum cliente de email instalado neste dispositivo",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void abrirWhatsApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://wa.me/" + WHATSAPP_NUMERO + "?text=Ol%C3%A1%2C+preciso+de+suporte+com+a+Jardulli+Calculadora"));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w("SuporteActivity", "WhatsApp não instalado", e);
            Toast.makeText(this,
                    "WhatsApp não está instalado neste dispositivo",
                    Toast.LENGTH_LONG).show();
        }
    }
}
