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

public class RodadosActivity extends AppCompatActivity {

    private Spinner spinnerRodadoDianteiro, spinnerEixoDianteiro, spinnerRodadoTraseiro, spinnerEixoTraseiro;
    private TextView textGrupoDianteiro, textGrupoTraseiro;
    private Button btnVoltar, btnAvancar;
    private DadosJardulli dadosJardulli;

    // Arrays gerais de tipos de eixo
    private final String[] tiposEixoStandard = {"SINGLE", "DUAL"};
    private final String[] tiposEixoJohnDeere = {"1.500 SINGLE", "1.500 DUAL", "ILS SINGLE", "ILS DUAL"};
    private final String[] tiposEixoNewHolland = {"FIXO PADRÃO", "FIXO REFORÇADO"};

    // Dados específicos por série - JOHN DEERE 8R
    private final String[] jd8rDianteiros = {
            "320/85R38", "14.9R34", "420/90R30", "16.9R30",
            "420/85R34", "480/70R30", "480/70R34", "540/65R34",
            "600/70R30", "LSW1000/40R32", "620/75R30", "600/70R28",
            "320/80R42", "570/75R34"
    };
    private final String[] jd8rTraseiros = {
            "380/90R50", "380/90R54", "480/80R50", "520/85R42",
            "520/85R42 162A8", "620/70R42", "620/70R46", "710/70R38",
            "710/70R42", "800/70R38", "850/60-38", "LSW1100/45R46",
            "14.9R50", "18.4R46", "20.8R42", "520/85R46"
    };
    private final int[] jd8rGruposDianteiros = {42, 42, 42, 42, 43, 42, 43, 43, 43, 0, 44, 42, 43, 44};
    private final int[] jd8rGruposTraseiros = {47, 48, 48, 47, 47, 47, 47, 47, 48, 48, 0, 47, 47, 47, 48, 48};

    // Dados específicos por série - NEW HOLAND T7
    private final String[] nht7Dianteiros = {
            "18.4-26 R1", "600/65 R28", "14.9-28 8PR R2", "18.4-26 R2"
    };
    private final String[] nht7Traseiros = {
            "24.5-32 R1", "20.8-38 R1 DUAL", "710/70R38", "23.1-34 8PR R2",
            "20.8-38 R2", "520/85R42"
    };
    private final int[] nht7GruposDianteiros = {0, 0, 0, 0};
    private final int[] nht7GruposTraseiros = {0, 0, 0, 0, 0, 0};

    // Dados específicos por série - NEW HOLAND T.8
    private final String[] nht8Dianteiros = {
            "420/90R30", "16.9R30 DUAL", "710/60R30 R1W", "750/55R3R1W",
            "480/70R34 R1W DUPLO", "750/60R30 R1W", "600/70R30", "LSW1000/40R32",
            "LSW 750/60R38", "600/65R28 RIW", "600/60R30.5 TRELLEBORG"
    };
    private final String[] nht8Traseiros = {
            "520/85R42 162A8 R1W DUAL", "710/70R41R1W DUAL", "850/60R38 1W TRELEBORG",
            "LSW1100/45R46", "LSW800/55R46 R1W DUAL", "20.8R42 R2 DUAL",
            "800/70R38 R1W DUAL", "900/60R38 R1W"
    };
    private final int[] nht8GruposDianteiros = {42, 42, 0, 0, 43, 0, 43, 0, 0, 42, 43};
    private final int[] nht8GruposTraseiros = {47, 48, 0, 0, 47, 47, 0, 0};

    // Dados específicos por série - CASE PUMA
    private final String[] casePumaDianteiros = {
            "600/65R28 A8 R1W", "16.9R30 A8 R1W", "16.9R30 R2"
    };
    private final String[] casePumaTraseiros = {
            "710/70R38 A8 R1W", "20.8R42 A8 R1W DUAL", "20.8R42 R2 DUAL", "520/85R42"
    };
    private final int[] casePumaGruposDianteiros = {0, 0, 0};
    private final int[] casePumaGruposTraseiros = {0, 0, 0, 0};

    // Dados específicos por série - CASE MAGUNUM
    private final String[] caseMagunumDianteiros = jd8rDianteiros;
    private final String[] caseMagunumTraseiros = jd8rTraseiros;
    private final int[] caseMagunumGruposDianteiros = jd8rGruposDianteiros;
    private final int[] caseMagunumGruposTraseiros = jd8rGruposTraseiros;

    // Dados específicos por série - MAGNUM AFS CONNECT
    private final String[] magnumAfsDianteiros = jd8rDianteiros;
    private final String[] magnumAfsTraseiros = jd8rTraseiros;
    private final int[] magnumAfsGruposDianteiros = jd8rGruposDianteiros;
    private final int[] magnumAfsGruposTraseiros = jd8rGruposTraseiros;

    // Dados específicos por série - VALTRA TCVT
    private final String[] valtraDianteiros = {
            "480/80 R26 - R1 - MICHELIN™", "540/65 R28 - R1 - TRELLEBORG ™",
            "14.9X28 - R2 - PIRELLI ™", "18.4X26 R1 - GOODYEAR ™"
    };
    private final String[] valtraTraseiros = {
            "680/75 R32 R1 - MICHELIN™", "800/65 R32 R1 - MICHELIN",
            "800/65 R32 R1 - TRELLEBORG ™", "23.1X30 R2 - PIRELLI ™"
    };
    private final int[] valtraGruposDianteiros = {0, 0, 0, 0};
    private final int[] valtraGruposTraseiros = {0, 0, 0, 0};

    // Dados específicos por série - JOHN DEERE 7J/M
    private final String[] jd7jmDianteiros = {
            "16.9 - 30 R1 - 10PR", "16.9 - 30 R1", "16.9R30 R2",
            "16.9R30 R2", "600/65R28 R1", "480/70R28 R1",
            "18.4-26 R1 10/12PR", "600/60-30.5 R1 - 153A8", "LSW 1000/40R32"
    };
    private final String[] jd7jmTraseiros = {
            "20.8 - 42 R1 - 10PR", "20.8 - 42 R1x2 - 10PR", "20.8R42 R2",
            "20.8R42 R2x2", "710/70R38", "800/65R32",
            "30.5L-32 R1 14PR", "850/60-38 R1 - 175A8", "LSW1100/45R46", "520/85R42"
    };
    private final int[] jd7jmGruposDianteiros = {42, 42, 42, 42, 42, 42, 42, 42, 0};
    private final int[] jd7jmGruposTraseiros = {47, 47, 47, 47, 47, 47, 47, 47, 0, 0};

    // Dados específicos por série - JOHN DEERE 6J/M
    private final String[] jd6jmDianteiros = {
            "14.9-24 R1 12L Florestal", "14.9-24 R1 8L", "14,9-26 R1 8L",
            "14.9-28 R1 8L", "16.9-24 R1 8L", "16,9-26 R1 8L"
    };
    private final String[] jd6jmTraseiros = {
            "18.4-34 R1 14L FL Florestal", "18.4-34 R1 10L FL", "18.4-38 R1 10L RP",
            "23.1-30 R1 10L FL", "20,8-38 R1 10L RP", "24,5-32 R1 10L RP"
    };
    private final int[] jd6jmGruposDianteiros = {0, 0, 0, 0, 0, 0};
    private final int[] jd6jmGruposTraseiros = {0, 0, 0, 0, 0, 0};

    // Arrays atuais baseados na série selecionada
    private String[] rodadosDianteirosAtuais;
    private String[] rodadosTraseirosAtuais;
    private int[] gruposDianteirosAtuais;
    private int[] gruposTraseirosAtuais;
    private String[] tiposEixoAtuais;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rodados);

        // Recupera dados da tela anterior
        dadosJardulli = getIntent().getParcelableExtra("DADOS_JARDULLI");
        if (dadosJardulli == null) {
            Toast.makeText(this, "Dados do trator não recebidos!", Toast.LENGTH_SHORT).show();
            Log.e("RODADOS_ACTIVITY", "Nenhum dado recebido da tela anterior");
            finish();
            return;
        }

        initViews();
        configurarRodadosPorSerie();
        setupSpinners();
        setupButtons();
    }

    private void initViews() {
        spinnerRodadoDianteiro = findViewById(R.id.spinnerRodadoDianteiro);
        spinnerEixoDianteiro = findViewById(R.id.spinnerEixoDianteiro);
        spinnerRodadoTraseiro = findViewById(R.id.spinnerRodadoTraseiro);
        spinnerEixoTraseiro = findViewById(R.id.spinnerEixoTraseiro);
        textGrupoDianteiro = findViewById(R.id.textGrupoDianteiro);
        textGrupoTraseiro = findViewById(R.id.textGrupoTraseiro);
        btnVoltar = findViewById(R.id.btnVoltar);
        btnAvancar = findViewById(R.id.btnAvancar);
    }

    private void configurarRodadosPorSerie() {
        String serie = dadosJardulli.getSerieTrator();

        if (serie == null) {
            Toast.makeText(this, "Série não identificada!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configura arrays baseados na série selecionada
        switch (serie) {
            case "John Deere 8R":
                rodadosDianteirosAtuais = jd8rDianteiros;
                rodadosTraseirosAtuais = jd8rTraseiros;
                gruposDianteirosAtuais = jd8rGruposDianteiros;
                gruposTraseirosAtuais = jd8rGruposTraseiros;
                tiposEixoAtuais = tiposEixoJohnDeere;
                break;
            case "New Holand T7":
                rodadosDianteirosAtuais = nht7Dianteiros;
                rodadosTraseirosAtuais = nht7Traseiros;
                gruposDianteirosAtuais = nht7GruposDianteiros;
                gruposTraseirosAtuais = nht7GruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "New Holand T.8":
                rodadosDianteirosAtuais = nht8Dianteiros;
                rodadosTraseirosAtuais = nht8Traseiros;
                gruposDianteirosAtuais = nht8GruposDianteiros;
                gruposTraseirosAtuais = nht8GruposTraseiros;
                tiposEixoAtuais = tiposEixoNewHolland;
                break;
            case "Case Puma":
                rodadosDianteirosAtuais = casePumaDianteiros;
                rodadosTraseirosAtuais = casePumaTraseiros;
                gruposDianteirosAtuais = casePumaGruposDianteiros;
                gruposTraseirosAtuais = casePumaGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "Case Magunum":
                rodadosDianteirosAtuais = caseMagunumDianteiros;
                rodadosTraseirosAtuais = caseMagunumTraseiros;
                gruposDianteirosAtuais = caseMagunumGruposDianteiros;
                gruposTraseirosAtuais = caseMagunumGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "Magnum Afs Connect":
                rodadosDianteirosAtuais = magnumAfsDianteiros;
                rodadosTraseirosAtuais = magnumAfsTraseiros;
                gruposDianteirosAtuais = magnumAfsGruposDianteiros;
                gruposTraseirosAtuais = magnumAfsGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "Valtra Tcvt":
                rodadosDianteirosAtuais = valtraDianteiros;
                rodadosTraseirosAtuais = valtraTraseiros;
                gruposDianteirosAtuais = valtraGruposDianteiros;
                gruposTraseirosAtuais = valtraGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "John Deere 7J/M":
                rodadosDianteirosAtuais = jd7jmDianteiros;
                rodadosTraseirosAtuais = jd7jmTraseiros;
                gruposDianteirosAtuais = jd7jmGruposDianteiros;
                gruposTraseirosAtuais = jd7jmGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            case "John Deere 6J/M":
                rodadosDianteirosAtuais = jd6jmDianteiros;
                rodadosTraseirosAtuais = jd6jmTraseiros;
                gruposDianteirosAtuais = jd6jmGruposDianteiros;
                gruposTraseirosAtuais = jd6jmGruposTraseiros;
                tiposEixoAtuais = tiposEixoStandard;
                break;
            default:
                Toast.makeText(this, "Série não suportada: " + serie, Toast.LENGTH_SHORT).show();
                finish();
                return;
        }

        Log.d("RODADOS_ACTIVITY", "Série configurada: " + serie);
    }

    private void setupSpinners() {
        // Rodado Dianteiro
        ArrayAdapter<String> adapterDianteiro = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rodadosDianteirosAtuais);
        adapterDianteiro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRodadoDianteiro.setAdapter(adapterDianteiro);
        spinnerRodadoDianteiro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textGrupoDianteiro.setText("Grupo: " + (gruposDianteirosAtuais[position] == 0 ? "N/A" : gruposDianteirosAtuais[position]));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Rodado Traseiro
        ArrayAdapter<String> adapterTraseiro = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rodadosTraseirosAtuais);
        adapterTraseiro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRodadoTraseiro.setAdapter(adapterTraseiro);
        spinnerRodadoTraseiro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textGrupoTraseiro.setText("Grupo: " + (gruposTraseirosAtuais[position] == 0 ? "N/A" : gruposTraseirosAtuais[position]));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Tipos de Eixo
        ArrayAdapter<String> adapterEixo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tiposEixoAtuais);
        adapterEixo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEixoDianteiro.setAdapter(adapterEixo);
        spinnerEixoTraseiro.setAdapter(adapterEixo);
    }

    private void setupButtons() {
        btnVoltar.setOnClickListener(v -> finish());

        btnAvancar.setOnClickListener(v -> {
            if (validarSelecoes()) {
                atualizarDadosRodados();
                Log.d("RODADOS_ACTIVITY", "Dados atualizados: " + dadosJardulli.toString());

                Intent intent = new Intent(RodadosActivity.this, LastroLiquidoActivity.class);
                intent.putExtra("DADOS_JARDULLI", dadosJardulli);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selecione todos os rodados!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void atualizarDadosRodados() {
        int posDianteiro = spinnerRodadoDianteiro.getSelectedItemPosition();
        int posTraseiro = spinnerRodadoTraseiro.getSelectedItemPosition();

        // Rodados usando setters
        dadosJardulli.setRodadoDianteiro(rodadosDianteirosAtuais[posDianteiro]);
        dadosJardulli.setGrupoDianteiro(gruposDianteirosAtuais[posDianteiro]);

        dadosJardulli.setRodadoTraseiro(rodadosTraseirosAtuais[posTraseiro]);
        dadosJardulli.setGrupoTraseiro(gruposTraseirosAtuais[posTraseiro]);

        // Pesos dos eixos baseados no tipo selecionado e série
        String eixoDianteiro = spinnerEixoDianteiro.getSelectedItem().toString();
        String eixoTraseiro = spinnerEixoTraseiro.getSelectedItem().toString();
        String serie = dadosJardulli.getSerieTrator();

        // ✅ SALVAR OS TIPOS DE EIXO
        dadosJardulli.setTipoEixoDianteiro(eixoDianteiro);
        dadosJardulli.setTipoEixoTraseiro(eixoTraseiro);

        // Definir pesos baseados na série e tipo de eixo
        definirPesosEixos(serie, eixoDianteiro, eixoTraseiro);

        // Log dos valores definidos
        Log.d("RODADOS_ACTIVITY", "Rodado Dianteiro: " + rodadosDianteirosAtuais[posDianteiro] +
                " | Grupo: " + gruposDianteirosAtuais[posDianteiro]);
        Log.d("RODADOS_ACTIVITY", "Rodado Traseiro: " + rodadosTraseirosAtuais[posTraseiro] +
                " | Grupo: " + gruposTraseirosAtuais[posTraseiro]);
        Log.d("RODADOS_ACTIVITY", "Eixo Dianteiro: " + eixoDianteiro + " = " + dadosJardulli.getPesoEixoDianteiro() + " kg");
        Log.d("RODADOS_ACTIVITY", "Eixo Traseiro: " + eixoTraseiro + " = " + dadosJardulli.getPesoEixoTraseiro() + " kg");
        Log.d("RODADOS_ACTIVITY", "Tipos de eixo salvos: " + eixoDianteiro + " / " + eixoTraseiro);
    }

    private void definirPesosEixos(String serie, String eixoDianteiro, String eixoTraseiro) {
        int pesoDianteiro = 0;
        int pesoTraseiro = 0;

        // JOHN DEERE
        if (serie.equals("John Deere 6J/M")) {
            String modelo = dadosJardulli.getTrator();
            int pesoBaseDianteiro = 0;
            int pesoBaseTraseiro = 0;

            // ✅ PESOS BASE POR MODELO ESPECÍFICO
            if (modelo != null) {
                switch (modelo) {
                    case "6100J": pesoBaseDianteiro = 1852; pesoBaseTraseiro = 2779; break;
                    case "6115J": pesoBaseDianteiro = 1852; pesoBaseTraseiro = 2779; break;
                    case "6125J": pesoBaseDianteiro = 1853; pesoBaseTraseiro = 2778; break;
                    case "6135J": pesoBaseDianteiro = 2016; pesoBaseTraseiro = 3024; break;
                    case "6150J": pesoBaseDianteiro = 2334; pesoBaseTraseiro = 3502; break;
                    case "6170J": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 4128; break;
                    case "6190J": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 4128; break;
                    case "6210J": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 4128; break;
                    case "6135M": pesoBaseDianteiro = 2016; pesoBaseTraseiro = 3024; break;
                    case "6150M": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 3502; break;
                    case "6170M": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 3502; break;
                    case "6190M": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 4128; break;
                    case "6210M": pesoBaseDianteiro = 2752; pesoBaseTraseiro = 4128; break;
                    default: pesoBaseDianteiro = 1852; pesoBaseTraseiro = 2779;
                }
            }

            // ✅ ACRÉSCIMO APENAS PARA EIXOS DUAL
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = pesoBaseDianteiro;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = pesoBaseDianteiro + 480;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = pesoBaseTraseiro;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = pesoBaseTraseiro + 360;
        }
        else if (serie.equals("John Deere 7J/M")) {
            // ✅ PESOS BASE + PESOS DOS EIXOS
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 3212 + 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 3212 + 360;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 5214 + 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 5214 + 600;
        }
        else if (serie.equals("John Deere 8R")) {
            if (eixoDianteiro.equals("1.500 SINGLE")) pesoDianteiro = 5148;
            else if (eixoDianteiro.equals("1.500 DUAL")) pesoDianteiro = 5898;
            else if (eixoDianteiro.equals("ILS SINGLE")) pesoDianteiro = 5500;
            else if (eixoDianteiro.equals("ILS DUAL")) pesoDianteiro = 6820;

            if (eixoTraseiro.equals("1.500 SINGLE")) pesoTraseiro = 6551;
            else if (eixoTraseiro.equals("1.500 DUAL")) pesoTraseiro = 8711;
            else if (eixoTraseiro.equals("ILS SINGLE")) pesoTraseiro = 6588;
            else if (eixoTraseiro.equals("ILS DUAL")) pesoTraseiro = 7900;
        }
        // NEW HOLLAND
        else if (serie.equals("New Holand T7")) {
            String modelo = dadosJardulli.getTrator();
            int pesoBaseDianteiro = 0;
            int pesoBaseTraseiro = 0;

            // ✅ PESOS BASE POR FAIXA DE MODELOS
            if (modelo != null) {
                switch (modelo) {
                    case "140": case "150": case "165": case "175": case "180":
                    case "190": case "195": case "205":
                        pesoBaseDianteiro = 2690;
                        pesoBaseTraseiro = 4035;
                        break;
                    case "240": case "245": case "260":
                        pesoBaseDianteiro = 3087;
                        pesoBaseTraseiro = 4630;
                        break;
                    default:
                        pesoBaseDianteiro = 2690;
                        pesoBaseTraseiro = 4035;
                }
            }

            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = pesoBaseDianteiro + 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = pesoBaseDianteiro + 360;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = pesoBaseTraseiro + 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = pesoBaseTraseiro + 760;
        }
        else if (serie.equals("New Holand T.8")) {
            if (eixoDianteiro.equals("FIXO PADRÃO")) pesoDianteiro = 4660;
            else if (eixoDianteiro.equals("FIXO REFORÇADO")) pesoDianteiro = 4735;

            if (eixoTraseiro.equals("FIXO PADRÃO")) pesoTraseiro = 6170;
            else if (eixoTraseiro.equals("FIXO REFORÇADO")) pesoTraseiro = 6250;
        }
        // CASE
        else if (serie.equals("Case Puma")) {
            // ✅ PESOS FIXOS POR SÉRIE
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 3305 + 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 3305 + 360;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 4413 + 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 4413 + 600;
        }
        else if (serie.equals("Case Magunum")) {
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 360;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 600;
        }
        // VALTRA
        else if (serie.equals("Valtra Tcvt")) {
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 360;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 600;
        }
        // MAGNUM (valores zerados conforme tabela)
        else if (serie.equals("Magnum Afs Connect")) {
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 0;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 0;
        }
        // PADRÃO PARA OUTRAS SÉRIES
        else {
            if (eixoDianteiro.equals("SINGLE")) pesoDianteiro = 0;
            else if (eixoDianteiro.equals("DUAL")) pesoDianteiro = 0;

            if (eixoTraseiro.equals("SINGLE")) pesoTraseiro = 0;
            else if (eixoTraseiro.equals("DUAL")) pesoTraseiro = 0;
        }

        dadosJardulli.setPesoEixoDianteiro(pesoDianteiro);
        dadosJardulli.setPesoEixoTraseiro(pesoTraseiro);
    }

    private boolean validarSelecoes() {
        return spinnerRodadoDianteiro.getSelectedItem() != null &&
                spinnerRodadoTraseiro.getSelectedItem() != null &&
                spinnerEixoDianteiro.getSelectedItem() != null &&
                spinnerEixoTraseiro.getSelectedItem() != null;
    }
}