package com.example.jardullicalculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AvancoRatioActivity extends AppCompatActivity {

    private EditText editVelocidadeTeorica;
    private EditText editVelocidadeReal;
    private TextView txtResultadoPatinagem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avanco_ratio);

        editVelocidadeTeorica = findViewById(R.id.editVelocidadeTeorica);
        editVelocidadeReal = findViewById(R.id.editVelocidadeReal);
        txtResultadoPatinagem = findViewById(R.id.txtResultadoPatinagem);

        Button btnCalcular = findViewById(R.id.btnCalcular);
        btnCalcular.setOnClickListener(v -> calcularPatinagem());

        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void calcularPatinagem() {
        String velTeStr = editVelocidadeTeorica.getText() != null
                ? editVelocidadeTeorica.getText().toString().trim() : "";
        String velReStr = editVelocidadeReal.getText() != null
                ? editVelocidadeReal.getText().toString().trim() : "";

        if (velTeStr.isEmpty() || velReStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double velocidadeTeorica = Double.parseDouble(velTeStr.replace(",", "."));
            double velocidadeReal = Double.parseDouble(velReStr.replace(",", "."));

            if (velocidadeTeorica <= 0) {
                Toast.makeText(this, "Velocidade teórica deve ser maior que zero", Toast.LENGTH_SHORT).show();
                return;
            }

            double patinagem = ((velocidadeTeorica - velocidadeReal) / velocidadeTeorica) * 100;

            String resultado;
            if (patinagem >= 0) {
                resultado = String.format(Locale.getDefault(), "Patinagem: %.1f%%", patinagem);
            } else {
                resultado = String.format(Locale.getDefault(), "Avanço: %.1f%%", Math.abs(patinagem));
            }

            String classificacao;
            double absPatinagem = Math.abs(patinagem);
            if (patinagem >= 0) {
                if (absPatinagem <= 8) {
                    classificacao = "✅ Patinagem dentro da faixa ideal (0-8%)";
                } else if (absPatinagem <= 15) {
                    classificacao = "⚠️ Patinagem moderada (8-15%)";
                } else {
                    classificacao = "❌ Patinagem excessiva (>15%)";
                }
            } else {
                if (absPatinagem <= 5) {
                    classificacao = "✅ Avanço dentro da faixa aceitável (0-5%)";
                } else {
                    classificacao = "⚠️ Avanço elevado (>5%)";
                }
            }

            txtResultadoPatinagem.setText(resultado + "\n\n" + classificacao);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valores inválidos", Toast.LENGTH_SHORT).show();
        }
    }
}
