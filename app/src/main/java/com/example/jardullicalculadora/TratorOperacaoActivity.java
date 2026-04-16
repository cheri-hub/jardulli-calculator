package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TratorOperacaoActivity extends AppCompatActivity {

    private Spinner spinnerSerie, spinnerModelo, spinnerEngate, spinnerOperacao;
    private TextView textPotencia;
    private EditText editPesoOperador;
    private Button btnAvancar, btnVoltar;

    // ✅ SÉRIES ATIVAS (6 SÉRIES TESTADAS E VALIDADAS)
    private final String[] series = {
            "John Deere 8R",           // ✅ TESTADO
            "New Holand T7",           // ✅ TESTADO
            "New Holand T.8",          // ✅ TESTADO
            "Case Puma",               // ✅ TESTADO
            // "Case Magunum",         // 🚫 DESATIVADO - NÃO TESTADO
            // "Magnum Afs Connect",   // 🚫 DESATIVADO - NÃO TESTADO
            // "Valtra Tcvt",          // 🚫 DESATIVADO - NÃO TESTADO
            "John Deere 7J/M",         // ✅ TESTADO
            "John Deere 6J/M"          // ✅ TESTADO
    };

    // Dados completos: Série -> Modelos (MANTER ORDEM ORIGINAL)
    private final String[][] modelosPorSerie = {
            // John Deere 8R [0]
            {"8250R", "8270R", "8295R", "8320R", "8335R", "8345R", "8370R", "8400R"},
            // New Holand T7 [1]
            {"140", "150", "165", "175", "180", "190", "195", "205", "240", "245", "260"},
            // New Holand T.8 [2]
            {"270", "295", "325", "355", "385", "430", "440"},
            // Case Puma [3]
            {"200", "215", "230"},
            // Case Magunum [4] - 🚫 DESATIVADO
            {"235", "257", "284", "312", "340"},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {"260", "290", "315", "340", "380", "400"},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {"195", "210", "230", "250"},
            // John Deere 7J/M [7]
            {"7195J", "7200J", "7205J", "7210J", "7215J", "7225J", "7230J", "7200M", "7215M", "7230M"},
            // John Deere 6J/M [8]
            {"6100J", "6115J", "6125J", "6135J", "6150J", "6170J", "6190J", "6210J", "6135M", "6150M", "6170M", "6190M", "6210M"}
    };

    // Potências CV por série e modelo - MANTER ORDEM ORIGINAL
    private final int[][] potenciasCV = {
            // John Deere 8R [0]
            {250, 270, 295, 320, 335, 345, 370, 400},
            // New Holand T7 [1]
            {141, 155, 165, 175, 182, 190, 195, 182, 197, 213, 234},
            // New Holand T.8 [2]
            {232, 250, 280, 311, 340, 379, 395},
            // Case Puma [3]
            {197, 213, 233},
            // Case Magunum [4] - 🚫 DESATIVADO
            {235, 257, 284, 312, 340},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {260, 290, 315, 340, 380, 400},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {195, 210, 230, 250},
            // John Deere 7J/M [7]
            {195, 200, 200, 210, 215, 225, 230, 200, 215, 230},
            // John Deere 6J/M [8]
            {100, 115, 125, 135, 150, 170, 190, 210, 135, 151, 170, 190, 210}
    };

    // Potências kW por série e modelo - MANTER ORDEM ORIGINAL
    private final double[][] potenciasKW = {
            // John Deere 8R [0]
            {210, 227, 248, 269, 281, 290, 311, 336},
            // New Holand T7 [1]
            {104, 114, 121, 104, 134, 114, 121, 134, 145, 157, 172},
            // New Holand T.8 [2]
            {199, 215, 241, 267, 292, 326, 340},
            // Case Puma [3]
            {148, 158, 173},
            // Case Magunum [4] - 🚫 DESATIVADO
            {173, 189, 209, 229, 250},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {250, 280, 311, 340, 380, 396},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {165, 180, 195, 210},
            // John Deere 7J/M [7]
            {163.8, 272, 168, 176.4, 180.6, 222.7, 193.2, 168, 180.6, 193.2},
            // John Deere 6J/M [8]
            {86, 99, 107, 113.4, 126, 142.8, 159.6, 176.4, 113.4, 126, 142.8, 159.6, 176.4}
    };

    // Pesos Máximos por série e modelo - MANTER ORDEM ORIGINAL
    private final double[][] pesosMaximos = {
            // John Deere 8R [0]
            {14296, 14696, 16192, 17600, 18425, 19008, 19805, 19805},
            // New Holand T7 [1]
            {7755, 8525, 9075, 9625, 10010, 10450, 10500, 10835, 10638, 11715, 12870},
            // New Holand T.8 [2]
            {12760, 13400, 14770, 16680, 16680, 18700, 18700},
            // Case Puma [3]
            {11200, 12040, 12870},
            // Case Magunum [4] - 🚫 DESATIVADO
            {12000, 13400, 14770, 15680, 16880},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {10110, 10110, 10110, 14148, 15800, 15800},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {10725, 11550, 12650, 13750},
            // John Deere 7J/M [7]
            {10725, 11000, 11400, 11800, 11825, 12375, 12650, 11400, 11825, 12650},
            // John Deere 6J/M [8]
            {5600, 6300, 7000, 7425, 8250, 9350, 10450, 11550, 7425, 8500, 9350, 10450, 11550}
    };

    // Capacidade de Diesel - MANTER ORDEM ORIGINAL
    private final int[][] capacidadeDiesel = {
            // John Deere 8R [0]
            {715, 715, 715, 715, 715, 715, 715, 715},
            // New Holand T7 [1]
            {314, 314, 314, 314, 314, 314, 314, 314, 405, 405, 405},
            // New Holand T.8 [2]
            {788, 788, 788, 788, 788, 788, 788},
            // Case Puma [3]
            {405, 405, 405},
            // Case Magunum [4] - 🚫 DESATIVADO
            {726, 726, 726, 726, 715},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {788, 788, 788, 788, 788, 768},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {510, 510, 510, 510},
            // John Deere 7J/M [7]
            {392, 392, 400, 400, 400, 400, 400, 430, 430, 430},
            // John Deere 6J/M [8]
            {185, 240, 240, 300, 300, 300, 372, 372, 372, 372, 372, 372, 372}
    };

    // Distância entre eixos - MANTER ORDEM ORIGINAL
    private final double[][] distanciaEntreEixos = {
            // John Deere 8R [0]
            {3020, 3020, 3020, 3020, 3020, 3020, 3020, 3020},
            // New Holand T7 [1]
            {2884, 2884, 2884, 2884, 2884, 2884, 2884, 2884, 2884, 2884, 2884},
            // New Holand T.8 [2]
            {3055, 3055, 3055, 3055, 3055, 3155, 3155},
            // Case Puma [3]
            {2884, 2884, 2884},
            // Case Magunum [4] - 🚫 DESATIVADO
            {3055, 3055, 3055, 3055, 3055},
            // Magnum Afs Connect [5] - 🚫 DESATIVADO
            {3055, 3055, 3055, 3055, 3055, 3055},
            // Valtra Tcvt [6] - 🚫 DESATIVADO
            {2973, 2973, 2973, 2973},
            // John Deere 7J/M [7]
            {2688, 2688, 2688, 2688, 2688, 2688, 2688, 2860, 2860, 2860},
            // John Deere 6J/M [8]
            {2400, 2400, 2400, 2650, 2650, 2688, 2688, 2688, 2688, 2650, 2688, 2688, 2688}
    };

    private final String[] pontosEngate = {"3 PONTOS", "B.T.O."};
    private final String[] operacoes = {"LEVE", "MÉDIO", "PESADO"};

    // ✅ MAPA DE CORRESPONDÊNCIA: 6 séries ativas -> índices originais
    private final int[] indiceParaOriginal = {0, 1, 2, 3, 7, 8}; // John Deere 8R[0], T7[1], T.8[2], Puma[3], 7J/M[7], 6J/M[8]

    private int serieSelecionada = 0;
    private int modeloSelecionado = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trator_operacao);

        // Inicialização dos componentes
        spinnerSerie = findViewById(R.id.spinnerSerie);
        spinnerModelo = findViewById(R.id.spinnerModelo);
        spinnerEngate = findViewById(R.id.spinnerEngate);
        spinnerOperacao = findViewById(R.id.spinnerOperacao);
        textPotencia = findViewById(R.id.textPotencia);
        editPesoOperador = findViewById(R.id.editPesoOperador);
        btnAvancar = findViewById(R.id.btnAvancar);
        btnVoltar = findViewById(R.id.btnVoltar);

        // Configuração dos Spinners
        configurarSpinners();

        // Botão Avançar
        btnAvancar.setOnClickListener(v -> {
            if (validarSelecoes()) {
                DadosJardulli dados = new DadosJardulli();

                // ✅ Converter índice da lista ATIVA para índice ORIGINAL
                int indiceOriginal = indiceParaOriginal[serieSelecionada];

                // Armazenar série e modelo
                dados.setSerieTrator(spinnerSerie.getSelectedItem().toString());
                dados.setTrator(spinnerModelo.getSelectedItem().toString());

                // Armazenar dados técnicos (usar índice ORIGINAL)
                dados.setPesoTrator(potenciasCV[indiceOriginal][modeloSelecionado]);
                dados.setPotenciaKW(potenciasKW[indiceOriginal][modeloSelecionado]);
                dados.setPesoMaximo(pesosMaximos[indiceOriginal][modeloSelecionado]);
                dados.setOperacao(spinnerOperacao.getSelectedItem().toString());
                dados.setPontoEngate(spinnerEngate.getSelectedItem().toString());

                // NOVOS DADOS: Capacidade de Diesel e Distância entre eixos
                dados.setCapacidadeDiesel(capacidadeDiesel[indiceOriginal][modeloSelecionado]);
                dados.setDistanciaEntreEixos(distanciaEntreEixos[indiceOriginal][modeloSelecionado]);

                // Armazenar peso do operador
                String pesoOperadorStr = editPesoOperador.getText().toString().trim();
                if (!pesoOperadorStr.isEmpty()) {
                    try {
                        int pesoOperador = Integer.parseInt(pesoOperadorStr);
                        dados.setPesoOperador(pesoOperador);
                    } catch (NumberFormatException e) {
                        dados.setPesoOperador(0);
                    }
                } else {
                    dados.setPesoOperador(0);
                }

                // Debug
                Log.d("TRATOR_OPERACAO", "Série ativa: " + serieSelecionada + " -> Original: " + indiceOriginal);
                Log.d("TRATOR_OPERACAO", "Enviando dados: " + dados.toString());

                Intent intent = new Intent(TratorOperacaoActivity.this, RodadosActivity.class);
                intent.putExtra("DADOS_JARDULLI", dados);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selecione todas as opções!", Toast.LENGTH_SHORT).show();
            }
        });

        // Botão Voltar
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void configurarSpinners() {
        // Spinner Série (apenas 6 séries ATIVAS)
        ArrayAdapter<String> adapterSerie = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, series);
        adapterSerie.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSerie.setAdapter(adapterSerie);

        spinnerSerie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serieSelecionada = position;
                atualizarModelos();
                atualizarInformacoes();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner Modelo
        ArrayAdapter<String> adapterModelo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{});
        adapterModelo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModelo.setAdapter(adapterModelo);

        spinnerModelo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modeloSelecionado = position;
                atualizarInformacoes();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner Engate
        ArrayAdapter<String> adapterEngate = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pontosEngate);
        adapterEngate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEngate.setAdapter(adapterEngate);

        // Spinner Operação
        ArrayAdapter<String> adapterOperacao = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, operacoes);
        adapterOperacao.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOperacao.setAdapter(adapterOperacao);
    }

    private void atualizarModelos() {
        // ✅ Converter índice da lista ATIVA para índice ORIGINAL
        int indiceOriginal = indiceParaOriginal[serieSelecionada];

        ArrayAdapter<String> adapterModelo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelosPorSerie[indiceOriginal]);
        adapterModelo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModelo.setAdapter(adapterModelo);

        // Resetar seleção do modelo
        spinnerModelo.setSelection(0);
        modeloSelecionado = 0;
    }

    private void atualizarInformacoes() {
        if (serieSelecionada >= 0 && modeloSelecionado >= 0) {
            // ✅ Converter índice da lista ATIVA para índice ORIGINAL
            int indiceOriginal = indiceParaOriginal[serieSelecionada];

            String info = "Potência: " + potenciasCV[indiceOriginal][modeloSelecionado] + " CV\n" +
                    "Potência KW: " + potenciasKW[indiceOriginal][modeloSelecionado] + " kW\n" +
                    "Peso Máximo: " + pesosMaximos[indiceOriginal][modeloSelecionado] + " kg\n" +
                    "Diesel: " + capacidadeDiesel[indiceOriginal][modeloSelecionado] + " L\n" +
                    "Entre-eixos: " + distanciaEntreEixos[indiceOriginal][modeloSelecionado] + " mm";
            textPotencia.setText(info);
        }
    }

    private boolean validarSelecoes() {
        return spinnerSerie.getSelectedItem() != null &&
                spinnerModelo.getSelectedItem() != null &&
                spinnerEngate.getSelectedItem() != null &&
                spinnerOperacao.getSelectedItem() != null;
    }
}