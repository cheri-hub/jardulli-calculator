package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LastroLiquidoActivity extends AppCompatActivity {

    private Spinner spinnerMultiplicadorDianteiro, spinnerMultiplicadorTraseiro;
    private TextView textRodadoDianteiroSelecionado, textRodadoTraseiroSelecionado;
    private TextView textResultadoDianteiro, textResultadoTraseiro;
    private Button btnVoltar, btnAvancar;
    private DadosJardulli dadosJardulli;

    private final Integer[] multiplicadores = {
            5, 10, 15, 20, 25, 30, 35, 40, 45, 50,
            55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    };

    // Variáveis para armazenar resultados calculados
    private int lastroDianteiroCalculado = 0;
    private int lastroTraseiroCalculado = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lastro_liquido);

        // Recupera os dados completos da tela anterior
        dadosJardulli = getIntent().getParcelableExtra("DADOS_JARDULLI");
        if (dadosJardulli == null) {
            Toast.makeText(this, "Dados não recebidos!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializa os elementos da UI
        initViews();

        // Exibe os rodados recebidos da tela anterior
        exibirDadosRecebidos();

        // Configura os spinners
        configurarSpinners();

        // Configura os listeners
        setupListeners();

        // Configura os botões
        setupButtons();
    }

    private void initViews() {
        textRodadoDianteiroSelecionado = findViewById(R.id.textRodadoDianteiroSelecionado);
        textRodadoTraseiroSelecionado = findViewById(R.id.textRodadoTraseiroSelecionado);
        spinnerMultiplicadorDianteiro = findViewById(R.id.spinnerMultiplicadorDianteiro);
        spinnerMultiplicadorTraseiro = findViewById(R.id.spinnerMultiplicadorTraseiro);
        textResultadoDianteiro = findViewById(R.id.textResultadoDianteiro);
        textResultadoTraseiro = findViewById(R.id.textResultadoTraseiro);
        btnVoltar = findViewById(R.id.btnVoltar);
        btnAvancar = findViewById(R.id.btnAvancar);
    }

    private void exibirDadosRecebidos() {
        textRodadoDianteiroSelecionado.setText("Rodado Dianteiro: " + dadosJardulli.getRodadoDianteiro());
        textRodadoTraseiroSelecionado.setText("Rodado Traseiro: " + dadosJardulli.getRodadoTraseiro());

        // Exibe também os grupos se necessário
        if (dadosJardulli.getGrupoDianteiro() != 0) {
            textRodadoDianteiroSelecionado.append(" (Grupo: " + dadosJardulli.getGrupoDianteiro() + ")");
        }
        if (dadosJardulli.getGrupoTraseiro() != 0) {
            textRodadoTraseiroSelecionado.append(" (Grupo: " + dadosJardulli.getGrupoTraseiro() + ")");
        }
    }

    private void configurarSpinners() {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, multiplicadores);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerMultiplicadorDianteiro.setAdapter(adapter);
        spinnerMultiplicadorTraseiro.setAdapter(adapter);

        // Selecionar um valor padrão para forçar o cálculo inicial
        spinnerMultiplicadorDianteiro.setSelection(0);
        spinnerMultiplicadorTraseiro.setSelection(0);
    }

    private void setupListeners() {
        // Listener para dianteiro
        spinnerMultiplicadorDianteiro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("LASTRO_LIQUIDO", "Spinner Dianteiro selecionado: " + position);
                calcularLastroDianteiro();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Listener para traseiro
        spinnerMultiplicadorTraseiro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("LASTRO_LIQUIDO", "Spinner Traseiro selecionado: " + position);
                calcularLastroTraseiro();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupButtons() {
        btnVoltar.setOnClickListener(v -> finish());

        btnAvancar.setOnClickListener(v -> {
            try {
                if (validarSelecoes()) {
                    // Atualiza os dados de lastro no objeto existente
                    atualizarDadosLastro();

                    // Envia para a próxima tela
                    enviarParaProximaTela();
                } else {
                    Toast.makeText(this, "Selecione todos os multiplicadores!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("LASTRO_LIQUIDO", "Erro ao avançar: " + e.getMessage());
                Toast.makeText(this, "Erro nos cálculos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void calcularLastroDianteiro() {
        try {
            if (spinnerMultiplicadorDianteiro.getSelectedItem() != null &&
                    dadosJardulli.getRodadoDianteiro() != null) {

                int multiplicador = (Integer) spinnerMultiplicadorDianteiro.getSelectedItem();
                String rodado = dadosJardulli.getRodadoDianteiro();
                String serie = dadosJardulli.getSerieTrator();

                Log.d("LASTRO_LIQUIDO", "Calculando dianteiro: " + rodado + " × " + multiplicador + " | Série: " + serie);

                int valorLastro = getValorLastroDianteiro(serie, rodado);
                Log.d("LASTRO_LIQUIDO", "Valor lastro dianteiro: " + valorLastro);

                if (valorLastro > 0) {
                    // DECIDE QUAL CÁLCULO USAR BASEADO NA SÉRIE E TIPO
                    if (usarCalculo1(serie, true)) { // true = dianteiro
                        // CÁLCULO 1: (valorLastro * multiplicador) / 75 * 2
                        lastroDianteiroCalculado = (valorLastro * multiplicador) / 75 * 2;
                        Log.d("LASTRO_LIQUIDO", "Usando Cálculo 1: (" + valorLastro + " × " + multiplicador + ") / 75 × 2 = " + lastroDianteiroCalculado);
                    } else {
                        // CÁLCULO 2: (valorLastro * multiplicador) / 40 * 2
                        lastroDianteiroCalculado = (valorLastro * multiplicador) / 40 * 2;
                        Log.d("LASTRO_LIQUIDO", "Usando Cálculo 2: (" + valorLastro + " × " + multiplicador + ") / 40 × 2 = " + lastroDianteiroCalculado);
                    }

                    textResultadoDianteiro.setText("Resultado Dianteiro: " + lastroDianteiroCalculado + " kg");
                } else {
                    lastroDianteiroCalculado = 0;
                    textResultadoDianteiro.setText("Lastro: 0 (não cadastrado)");
                    Log.w("LASTRO_LIQUIDO", "Lastro não cadastrado para: " + rodado);
                }
            }
        } catch (Exception e) {
            Log.e("LASTRO_LIQUIDO", "Erro cálculo dianteiro: " + e.getMessage());
            textResultadoDianteiro.setText("Erro no cálculo");
            lastroDianteiroCalculado = 0;
        }
    }

    private void calcularLastroTraseiro() {
        try {
            if (spinnerMultiplicadorTraseiro.getSelectedItem() != null &&
                    dadosJardulli.getRodadoTraseiro() != null) {

                int multiplicador = (Integer) spinnerMultiplicadorTraseiro.getSelectedItem();
                String rodado = dadosJardulli.getRodadoTraseiro();
                String serie = dadosJardulli.getSerieTrator();

                Log.d("LASTRO_LIQUIDO", "Calculando traseiro: " + rodado + " × " + multiplicador + " | Série: " + serie);

                int valorLastro = getValorLastroTraseiro(serie, rodado);
                Log.d("LASTRO_LIQUIDO", "Valor lastro traseiro: " + valorLastro);

                if (valorLastro > 0) {
                    // DECIDE QUAL CÁLCULO USAR BASEADO NA SÉRIE E TIPO
                    if (usarCalculo1(serie, false)) { // false = traseiro
                        // CÁLCULO 1: (valorLastro * multiplicador) / 75 * 2
                        lastroTraseiroCalculado = (valorLastro * multiplicador) / 75 * 2;
                        Log.d("LASTRO_LIQUIDO", "Usando Cálculo 1: (" + valorLastro + " × " + multiplicador + ") / 75 × 2 = " + lastroTraseiroCalculado);
                    } else {
                        // CÁLCULO 2: (valorLastro * multiplicador) / 40 * 2
                        lastroTraseiroCalculado = (valorLastro * multiplicador) / 40 * 2;
                        Log.d("LASTRO_LIQUIDO", "Usando Cálculo 2: (" + valorLastro + " × " + multiplicador + ") / 40 × 2 = " + lastroTraseiroCalculado);
                    }

                    textResultadoTraseiro.setText("Resultado Traseiro: " + lastroTraseiroCalculado + " kg");
                } else {
                    lastroTraseiroCalculado = 0;
                    textResultadoTraseiro.setText("Lastro: 0 (não cadastrado)");
                    Log.w("LASTRO_LIQUIDO", "Lastro não cadastrado para: " + rodado);
                }
            }
        } catch (Exception e) {
            Log.e("LASTRO_LIQUIDO", "Erro cálculo traseiro: " + e.getMessage());
            textResultadoTraseiro.setText("Erro no cálculo");
            lastroTraseiroCalculado = 0;
        }
    }

    // DECIDE QUAL CÁLCULO USAR BASEADO NA SÉRIE E TIPO (DIANTEIRO/TRASEIRO)
    private boolean usarCalculo1(String serie, boolean isDianteiro) {
        if (serie == null) return false; // Padrão: Cálculo 2

        // REGRA ESPECIAL: John Deere 8R - Dianteiro usa Cálculo 2, Traseiro usa Cálculo 1
        if ("John Deere 8R".equals(serie)) {
            return !isDianteiro; // true para traseiro, false para dianteiro
        }

        switch (serie) {
            case "New Holand T7":
            case "New Holand T.8":
            case "Case Magunum":
            case "Valtra Tcvt":
            case "John Deere 6J/M":
                return true; // Usar Cálculo 1 (÷75×2)
            default:
                return false;  // Usar Cálculo 2 (÷40×2) - Case Puma, Magnum Afs Connect, John Deere 7J/M
        }
    }

    // VALORES DIANTEIRO POR SÉRIE
    private int getValorLastroDianteiro(String serie, String rodado) {
        if (serie == null) return 0;

        switch (serie) {
            case "John Deere 8R":
                switch (rodado) {
                    case "320/85R38": return 172;
                    case "14.9R34": return 0;
                    case "420/90R30": return 334;
                    case "16.9R30": return 0;
                    case "420/85R34": return 328;
                    case "480/70R30": return 172;
                    case "480/70R34": return 176;
                    case "540/65R34": return 156;
                    case "600/70R30": return 245;
                    case "LSW1000/40R32": return 754;
                    case "620/75R30": return 700;
                    case "600/70R28": return 448;
                    case "320/80R42": return 0;
                    case "570/75R34": return 0;
                    default: return 0;
                }

            case "New Holand T7":
                switch (rodado) {
                    case "18.4-26 R1": return 280;
                    case "600/65 R28": return 408;
                    case "14.9-28 8PR R2": return 180;
                    case "18.4-26 R2": return 280;
                    default: return 0;
                }

            case "New Holand T.8":
                switch (rodado) {
                    case "420/90R30": return 334;
                    case "16.9R30 DUAL": return 250;
                    case "710/60R30 R1W": return 420;
                    case "750/55R3R1W": return 420;
                    case "480/70R34 R1W DUPLO": return 474;
                    case "750/60R30 R1W": return 448;
                    case "600/70R30": return 458;
                    case "LSW1000/40R32": return 754;
                    case "LSW 750/60R38": return 520;
                    case "600/65R28 RIW": return 448;
                    case "600/60R30.5 TRELLEBORG": return 420;
                    default: return 0;
                }

            case "Case Puma":
                switch (rodado) {
                    case "600/65R28 A8 R1W": return 218;
                    case "16.9R30 A8 R1W": return 339;
                    case "16.9R30 R2": return 340;
                    default: return 0;
                }

            case "Case Magunum":
                switch (rodado) {
                    case "320/85R38": return 172;
                    case "14.9R34": return 0;
                    case "420/90R30": return 334;
                    case "16.9R30": return 0;
                    case "420/85R34": return 328;
                    case "480/70R30": return 340;
                    case "480/70R34": return 381;
                    case "540/65R34": return 156;
                    case "600/70R30": return 458;
                    case "LSW1000/40R32": return 754;
                    case "620/75R30": return 700;
                    case "600/70R28": return 448;
                    case "320/80R42": return 0;
                    case "570/75R34": return 0;
                    default: return 0;
                }

            case "Magnum Afs Connect":
                switch (rodado) {
                    case "320/85R38": return 172;
                    case "14.9R34": return 0;
                    case "420/90R30": return 334;
                    case "16.9R30": return 0;
                    case "420/85R34": return 328;
                    case "480/70R30": return 340;
                    case "480/70R34": return 381;
                    case "540/65R34": return 156;
                    case "600/70R30": return 245;
                    case "LSW1000/40R32": return 754;
                    case "620/75R30": return 700;
                    case "600/70R28": return 448;
                    case "320/80R42": return 0;
                    case "570/75R34": return 0;
                    default: return 0;
                }

            case "Valtra Tcvt":
                switch (rodado) {
                    case "480/80 R26 - R1 - MICHELIN™": return 297;
                    case "540/65 R28 - R1 -TRELLEBORG ™": return 318;
                    case "14.9X28 - R2 - PIRELLI ™": return 180;
                    case "18.4X26 R1 -GOODYEAR ™": return 320;
                    case "600/55-30.5 - R1 - TRELLEBORG ™": return 396;
                    case "600/65 R28 - R1 - TRELLEBORG ™": return 396;
                    case "600/65 R28 - R1 - TL - MICHELIN™": return 396;
                    case "600/65 R28 - R1 -PIRELLI ™": return 396;
                    case "600/65 R28 - R1W - FIRESTONE": return 396;
                    case "600/65 R28 - R1 - GOODYEAR": return 396;
                    case "18.4X26 R1 - GOODYEAR ™": return 300;
                    case "16.9R30 R1 - MICHELIN™": return 150;
                    case "16.9X30 R1 - FIRESTONE™": return 150;
                    case "600/55 30.5 - R1 - TRELLEBORG ™": return 396;
                    case "420/85 R30 - R1 - TRELLEBORG ™": return 297;
                    case "600/65 R28 - R1 - PIRELLI ™": return 396;
                    case "600/65 R28 - R1W - FIRESTONE™": return 396;
                    case "420/85 R30 - R1 - PIRELLI ™": return 300;
                    case "600/65 R28 - R1 - GOODYEAR ™": return 396;
                    case "540/65 R28 - R1 - TRELLEBORG ™": return 318;
                    default: return 0;
                }

            case "John Deere 7J/M":
                switch (rodado) {
                    case "16.9 - 30 R1 - 10PR": return 339;
                    case "16.9 - 30 R1": return 339;
                    case "16.9R30 R2": return 339;
                    case "600/65R28 R1": return 220;
                    case "480/70R28 R1": return 344;
                    case "18.4-26 R1 10/12PR": return 366;
                    case "600/60-30.5 R1 - 153A8": return 468;
                    case "lsw 1000/40r32": return 754;
                    default: return 0;
                }

            case "John Deere 6J/M":
                switch (rodado) {
                    case "14.9-24 R1 12L Florestal": return 150;
                    case "14.9-24 R1 8L": return 150;
                    case "14,9-26 R1 8L": return 170;
                    case "14.9-28 R1 8L": return 180;
                    case "16.9-24 R1 8L": return 234;
                    case "16,9-26 R1 8L": return 220;
                    case "16,9-28 R1 10L": return 201;
                    case "18,4-26 R1 10/12L 10B": return 280;
                    case "18,4-26 R1 10L Florestal": return 300;
                    case "380/85R24 R1W 131A8": return 162;
                    case "480/80R26": return 295;
                    case "600/65R28 R1": return 220;
                    case "16,9R30 R2 141A8": return 276;
                    default: return 0;
                }

            default:
                return 0;
        }
    }

    // VALORES TRASEIRO POR SÉRIE
    private int getValorLastroTraseiro(String serie, String rodado) {
        if (serie == null) return 0;

        switch (serie) {
            case "John Deere 8R":
                switch (rodado) {
                    case "380/90R50": return 195;
                    case "380/90R54": return 194;
                    case "480/80R50": return 288;
                    case "520/85R42": return 349;
                    case "520/85R42 162A8": return 375;
                    case "620/70R42": return 411;
                    case "620/70R46": return 421;
                    case "710/70R38": return 909;
                    case "710/70R42": return 909;
                    case "800/70R38": return 1087;
                    case "850/60-38": return 650;
                    case "LSW1100/45R46": return 1484;
                    case "14.9R50": return 0;
                    case "18.4R46": return 0;
                    case "20.8R42": return 0;
                    case "520/85R46": return 0;
                    default: return 0;
                }

            case "New Holand T7":
                switch (rodado) {
                    case "24.5-32 R1": return 600;
                    case "20.8-38 R1 DUAL": return 542;
                    case "710/70R38": return 836;
                    case "23.1-34 8PR R2": return 566;
                    case "20.8-38 R2": return 542;
                    case "520/85R42": return 590;
                    default: return 0;
                }

            case "New Holand T.8":
                switch (rodado) {
                    case "520/85R42 162A8 R1W DUAL": return 375;
                    case "710/70R41R1W DUAL": return 558;
                    case "850/60R38 1W TRELEBORG": return 650;
                    case "LSW1100/45R46": return 1484;
                    case "LSW800/55R46 R1W DUAL": return 901;
                    case "20.8R42 R2 DUAL": return 560;
                    case "800/70R38 R1W DUAL": return 580;
                    case "900/60R38 R1W": return 545;
                    default: return 0;
                }

            case "Case Puma":
                switch (rodado) {
                    case "710/70R38 A8 R1W": return 450;
                    case "20.8R42 A8 R1W DUAL": return 483;
                    case "20.8R42 R2 DUAL": return 480;
                    case "520/85R42": return 590;
                    default: return 0;
                }

            case "Case Magunum":
                switch (rodado) {
                    case "380/90R50": return 195;
                    case "380/90R54": return 194;
                    case "480/80R50": return 288;
                    case "520/85R42": return 349;
                    case "520/85R42 162A8": return 375;
                    case "620/70R42": return 411;
                    case "620/70R46": return 421;
                    case "710/70R38": return 495;
                    case "710/70R42": return 558;
                    case "800/70R38": return 1087;
                    case "850/60-38": return 650;
                    case "LSW1100/45R46": return 1484;
                    case "14.9R50": return 0;
                    case "18.4R46": return 0;
                    case "20.8R42": return 0;
                    case "520/85R46": return 0;
                    default: return 0;
                }

            case "Magnum Afs Connect":
                switch (rodado) {
                    case "380/90R50": return 195;
                    case "380/90R54": return 194;
                    case "480/80R50": return 288;
                    case "520/85R42": return 349;
                    case "520/85R42 162A8": return 375;
                    case "620/70R42": return 411;
                    case "620/70R46": return 421;
                    case "710/70R38": return 495;
                    case "710/70R42": return 485;
                    case "800/70R38": return 1087;
                    case "850/60-38": return 650;
                    case "LSW1100/45R46": return 1484;
                    case "14.9R50": return 0;
                    case "18.4R46": return 0;
                    case "20.8R42": return 0;
                    case "520/85R46": return 0;
                    default: return 0;
                }

            case "Valtra Tcvt":
                switch (rodado) {
                    case "680/75 R32 R1 - MICHELIN™": return 800;
                    case "800/65 R32 R1 - MICHELIN": return 1087;
                    case "800/65 R32 R1 - TRELLEBORG ™": return 1087;
                    case "23.1X30 R2 - PIRELLI ™": return 821;
                    case "30.5L-32R1 - GOODYEAR": return 821;
                    case "24.5-32R1 - GOODYEAR ™": return 600;
                    case "24.5-32R1 - FIRESTONE™": return 600;
                    case "24.5-32R1 - PIRELLI ™": return 600;
                    case "20.8X38 R1 - PIRELLI ™": return 560;
                    case "DUPLA 20.8X38 R1 - PIRELLI ™": return 560;
                    case "710/65-38 R1 - TRELLEBORG ™": return 836;
                    case "710/70 R38 R1 - TRELLEBORG ™": return 836;
                    case "650/75 R38 R1 - MICHELIN™": return 600;
                    case "710/70 R38 R1 - MICHELIN™": return 836;
                    case "710/70 R38 R1 - PIRELLI ™": return 836;
                    case "710/70 R38 R1W - FIRESTONE™": return 836;
                    case "710/70 R38 R1W - GOODYEAR ™": return 836;
                    case "800/65 R32 R1 -TRELLEBORG ™": return 1087;
                    default: return 0;
                }

            case "John Deere 7J/M":
                switch (rodado) {
                    case "20.8 - 42 R1 - 10PR": return 481;
                    case "20.8 - 42 R1x2 - 10PR": return 482;
                    case "20.8R42 R2": return 483;
                    case "20.8R42 R2x2": return 480;
                    case "710/70R38": return 446;
                    case "800/65R32": return 580;
                    case "30.5L-32 R1 14PR": return 750;
                    case "850/60-38 R1 - 175A8": return 930;
                    case "LSW1100/45R46": return 1484;
                    case "520/85R42": return 340;
                    default: return 0;
                }

            case "John Deere 6J/M":
                switch (rodado) {
                    case "18.4-34 R1 14L FL Florestal": return 379;
                    case "18.4-34 R1 10L FL": return 360;
                    case "18.4-38 R1 10L RP": return 416;
                    case "23.1-30 R1 10L FL": return 540;
                    case "20,8-38 R1 10L RP": return 530;
                    case "24,5-32 R1 10L RP": return 643;
                    case "30,5L-32 R1 14L RP": return 643;
                    case "710/70R38 R1W": return 836;
                    case "600/70R30 R1W 152A8": return 458;
                    case "650/75R32 R1W": return 346;
                    case "20,8-38 R1 10L RP (Duplo)": return 530;
                    default: return 0;
                }

            default:
                return 0;
        }
    }

    private void atualizarDadosLastro() {
        // Usa os valores calculados diretamente (não precisa extrair da TextView)
        dadosJardulli.setLastroDianteiro(lastroDianteiroCalculado);
        dadosJardulli.setLastroTraseiro(lastroTraseiroCalculado);

        Log.d("LASTRO_LIQUIDO", "Lastro Dianteiro salvo: " + dadosJardulli.getLastroDianteiro());
        Log.d("LASTRO_LIQUIDO", "Lastro Traseiro salvo: " + dadosJardulli.getLastroTraseiro());
    }

    private void enviarParaProximaTela() {
        Intent intent = new Intent(LastroLiquidoActivity.this, PesoFrontalActivity.class);
        intent.putExtra("DADOS_JARDULLI", dadosJardulli);
        startActivity(intent);
    }

    private boolean validarSelecoes() {
        return spinnerMultiplicadorDianteiro.getSelectedItem() != null &&
                spinnerMultiplicadorTraseiro.getSelectedItem() != null;
    }
}