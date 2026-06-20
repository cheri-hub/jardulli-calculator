package com.example.jardullicalculadora;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PesoFrontalActivity extends AppCompatActivity {

    // Spinners para quantidades
    private Spinner spinnerQtdDianteiro1, spinnerQtdDianteiro2;
    private Spinner spinnerQtdTraseiro1, spinnerQtdTraseiro2, spinnerQtdTraseiro3, spinnerQtdTraseiro4;
    private Spinner spinnerCabine;

    private TextView textResultadoDianteiro, textResultadoTraseiro, textPesoTotal;
    private TextView textPesoDianteiro1, textPesoDianteiro2;
    private TextView textPesoTraseiro1, textPesoTraseiro2, textPesoTraseiro3, textPesoTraseiro4;

    private Button btnAvancar, btnRetornar;
    private DadosJardulli dadosJardulli;

    private final Integer[] multiplicadores = {0, 1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
    private final String[] opcoesCabine = {"Não", "Sim"};

    // Variáveis para cálculos
    private int pesoDianteiroCalculado = 0;
    private int pesoTraseiroCalculado = 0;
    private int pesoTotalCalculado = 0;

    private static final String TAG = "PesoFrontalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peso_frontal);

        // Recebe os dados acumulados da tela anterior
        dadosJardulli = getIntent().getParcelableExtra("DADOS_JARDULLI");
        if (dadosJardulli == null) {
            Toast.makeText(this, "Dados não recebidos!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializa os componentes
        initViews();

        // Configura os spinners
        configurarSpinners();

        // Configura os listeners
        setupListeners();

        // Configura os botões
        setupButtons();

        // Configura textos iniciais
        configurarTextosPesosBase();
    }

    private void initViews() {
        // Spinners dianteiros
        spinnerQtdDianteiro1 = findViewById(R.id.spinnerQtdDianteiro1);
        spinnerQtdDianteiro2 = findViewById(R.id.spinnerQtdDianteiro2);

        // Spinners traseiros
        spinnerQtdTraseiro1 = findViewById(R.id.spinnerQtdTraseiro1);
        spinnerQtdTraseiro2 = findViewById(R.id.spinnerQtdTraseiro2);
        spinnerQtdTraseiro3 = findViewById(R.id.spinnerQtdTraseiro3);
        spinnerQtdTraseiro4 = findViewById(R.id.spinnerQtdTraseiro4);

        // Spinner cabine
        spinnerCabine = findViewById(R.id.spinnerCabine);

        // TextViews de resultado
        textResultadoDianteiro = findViewById(R.id.textResultadoDianteiro);
        textResultadoTraseiro = findViewById(R.id.textResultadoTraseiro);
        textPesoTotal = findViewById(R.id.textPesoTotal);

        // TextViews de descrição dos pesos
        textPesoDianteiro1 = findViewById(R.id.textPesoDianteiro1);
        textPesoDianteiro2 = findViewById(R.id.textPesoDianteiro2);
        textPesoTraseiro1 = findViewById(R.id.textPesoTraseiro1);
        textPesoTraseiro2 = findViewById(R.id.textPesoTraseiro2);
        textPesoTraseiro3 = findViewById(R.id.textPesoTraseiro3);
        textPesoTraseiro4 = findViewById(R.id.textPesoTraseiro4);

        // Botões
        btnAvancar = findViewById(R.id.btnAvancar);
        btnRetornar = findViewById(R.id.btnRetornar);
    }

    private void configurarSpinners() {
        // Configura multiplicadores para todos os spinners de quantidade
        ArrayAdapter<Integer> adapterMultiplicador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, multiplicadores);
        adapterMultiplicador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerQtdDianteiro1.setAdapter(adapterMultiplicador);
        spinnerQtdDianteiro2.setAdapter(adapterMultiplicador);
        spinnerQtdTraseiro1.setAdapter(adapterMultiplicador);
        spinnerQtdTraseiro2.setAdapter(adapterMultiplicador);
        spinnerQtdTraseiro3.setAdapter(adapterMultiplicador);
        spinnerQtdTraseiro4.setAdapter(adapterMultiplicador);

        // Configura spinner da cabine
        ArrayAdapter<String> adapterCabine = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opcoesCabine);
        adapterCabine.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCabine.setAdapter(adapterCabine);
    }

    private void configurarTextosPesosBase() {
        String serie = dadosJardulli.getSerieTrator();

        if (serie == null) {
            Toast.makeText(this, "Série não identificada!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configura textos dos dianteiros
        String[] descDianteiros = getDescricoesDianteiros(serie);
        int[] pesosDianteiros = getPesosDianteiros(serie);

        configurarLinhaPeso(textPesoDianteiro1, spinnerQtdDianteiro1, descDianteiros[0], pesosDianteiros[0]);
        configurarLinhaPeso(textPesoDianteiro2, spinnerQtdDianteiro2, descDianteiros[1], pesosDianteiros[1]);

        // Configura textos dos traseiros
        String[] descTraseiros = getDescricoesTraseiros(serie);
        int[] pesosTraseiros = getPesosTraseiros(serie);

        configurarLinhaPeso(textPesoTraseiro1, spinnerQtdTraseiro1, descTraseiros[0], pesosTraseiros[0]);
        configurarLinhaPeso(textPesoTraseiro2, spinnerQtdTraseiro2, descTraseiros[1], pesosTraseiros[1]);
        configurarLinhaPeso(textPesoTraseiro3, spinnerQtdTraseiro3, descTraseiros[2], pesosTraseiros[2]);
        configurarLinhaPeso(textPesoTraseiro4, spinnerQtdTraseiro4, descTraseiros[3], pesosTraseiros[3]);

        calcularPesos();
    }

    private void configurarLinhaPeso(TextView textView, Spinner spinner, String descricao, int peso) {
        textView.setText(descricao + " (" + peso + " kg)");

        View linha = getLinhaContainer(textView, spinner);
        if (linha == null) {
            Log.w(TAG, "Linha de peso sem container visivel: " + descricao);
            return;
        }

        if (!isPesoDisponivel(descricao, peso)) {
            spinner.setSelection(0);
            linha.setVisibility(View.GONE);
            return;
        }

        linha.setVisibility(View.VISIBLE);
    }

    private boolean isPesoDisponivel(String descricao, int peso) {
        return descricao != null && !"N/A".equalsIgnoreCase(descricao.trim()) && peso > 0;
    }

    private View getLinhaContainer(TextView textView, Spinner spinner) {
        ViewParent spinnerParent = spinner.getParent();
        if (spinnerParent instanceof View) {
            return (View) spinnerParent;
        }

        ViewParent textParent = textView.getParent();
        if (textParent instanceof View) {
            return (View) textParent;
        }

        return null;
    }

    private String[] getDescricoesDianteiros(String serie) {
        String[] descricoes = new String[2];

        switch (serie) {
            case "John Deere 8R":
                descricoes[0] = "50KG";
                descricoes[1] = "N/A";
                break;
            case "New Holand T7":
                descricoes[0] = "45 KG";
                descricoes[1] = "450KG";
                break;
            case "New Holand T.8":
                descricoes[0] = "40 KG";
                descricoes[1] = "N/A";
                break;
            case "Case Puma":
                descricoes[0] = "45 KG";
                descricoes[1] = "250 kg";
                break;
            case "Case Magunum":
                descricoes[0] = "45 KG";
                descricoes[1] = "alongador";
                break;
            case "Magnum Afs Connect":
                descricoes[0] = "45 KG";
                descricoes[1] = "N/A";
                break;
            case "Valtra Tcvt":
                descricoes[0] = "55 KG";
                descricoes[1] = "265 kg";
                break;
            case "John Deere 7J/M":
                descricoes[0] = "50 KG";
                descricoes[1] = "140 kg";
                break;
            case "John Deere 6J/M":
                descricoes[0] = "PESO MALA";
                descricoes[1] = "ALONGADOR";
                break;
            default:
                descricoes[0] = "N/A";
                descricoes[1] = "N/A";
        }

        return descricoes;
    }

    private int[] getPesosDianteiros(String serie) {
        int[] pesos = new int[2];

        switch (serie) {
            case "John Deere 8R":
                pesos[0] = 50; pesos[1] = 0;
                break;
            case "New Holand T7":
                pesos[0] = 45; pesos[1] = 450;
                break;
            case "New Holand T.8":
                pesos[0] = 40; pesos[1] = 0;
                break;
            case "Case Puma":
                pesos[0] = 45; pesos[1] = 250;
                break;
            case "Case Magunum":
                pesos[0] = 45; pesos[1] = 80;
                break;
            case "Magnum Afs Connect":
                pesos[0] = 45; pesos[1] = 0;
                break;
            case "Valtra Tcvt":
                pesos[0] = 55; pesos[1] = 265;
                break;
            case "John Deere 7J/M":
                pesos[0] = 50; pesos[1] = 140;
                break;
            case "John Deere 6J/M":
                pesos[0] = 50; pesos[1] = 80;
                break;
            default:
                pesos[0] = 0; pesos[1] = 0;
        }

        return pesos;
    }

    private String[] getDescricoesTraseiros(String serie) {
        String[] descricoes = new String[4];

        switch (serie) {
            case "John Deere 8R":
                descricoes[0] = "72 KG"; descricoes[1] = "205 KG";
                descricoes[2] = "centro"; descricoes[3] = "625 KG";
                break;
            case "New Holand T7":
                descricoes[0] = "91 KG"; descricoes[1] = "227 KG";
                descricoes[2] = "N/A"; descricoes[3] = "N/A";
                break;
            case "New Holand T.8":
                descricoes[0] = "91 KG"; descricoes[1] = "225 KG";
                descricoes[2] = "454 KG"; descricoes[3] = "N/A";
                break;
            case "Case Puma":
                descricoes[0] = "91KG"; descricoes[1] = "227KG";
                descricoes[2] = "N/A"; descricoes[3] = "N/A";
                break;
            case "Case Magunum":
                descricoes[0] = "91 KG"; descricoes[1] = "227 KG";
                descricoes[2] = "454 KG"; descricoes[3] = "N/A";
                break;
            case "Magnum Afs Connect":
                descricoes[0] = "PESO 91 KG"; descricoes[1] = "PESO 227 KG";
                descricoes[2] = "PESO 454 KG"; descricoes[3] = "N/A";
                break;
            case "Valtra Tcvt":
                descricoes[0] = "80 KG"; descricoes[1] = "N/A";
                descricoes[2] = "N/A"; descricoes[3] = "N/A";
                break;
            case "John Deere 7J/M":
                descricoes[0] = "72 KG"; descricoes[1] = "205 KG";
                descricoes[2] = "Centro da Roda"; descricoes[3] = "N/A";
                break;
            case "John Deere 6J/M":
                descricoes[0] = "49 KG"; descricoes[1] = "72 KG";
                descricoes[2] = "205 KG"; descricoes[3] = "N/A";
                break;
            default:
                descricoes[0] = "N/A"; descricoes[1] = "N/A";
                descricoes[2] = "N/A"; descricoes[3] = "N/A";
        }

        return descricoes;
    }

    private int[] getPesosTraseiros(String serie) {
        int[] pesos = new int[4];

        switch (serie) {
            case "John Deere 8R":
                pesos[0] = 72; pesos[1] = 205; pesos[2] = 160; pesos[3] = 625;
                break;
            case "New Holand T7":
                pesos[0] = 91; pesos[1] = 227; pesos[2] = 0; pesos[3] = 0;
                break;
            case "New Holand T.8":
                pesos[0] = 91; pesos[1] = 225; pesos[2] = 454; pesos[3] = 0;
                break;
            case "Case Puma":
                pesos[0] = 91; pesos[1] = 227; pesos[2] = 0; pesos[3] = 0;
                break;
            case "Case Magunum":
                pesos[0] = 91; pesos[1] = 227; pesos[2] = 454; pesos[3] = 0;
                break;
            case "Magnum Afs Connect":
                pesos[0] = 91; pesos[1] = 227; pesos[2] = 454; pesos[3] = 0;
                break;
            case "Valtra Tcvt":
                pesos[0] = 80; pesos[1] = 0; pesos[2] = 0; pesos[3] = 0;
                break;
            case "John Deere 7J/M":
                pesos[0] = 72; pesos[1] = 205; pesos[2] = 160; pesos[3] = 0;
                break;
            case "John Deere 6J/M":
                pesos[0] = 49; pesos[1] = 72; pesos[2] = 205; pesos[3] = 0;
                break;
            default:
                pesos[0] = 0; pesos[1] = 0; pesos[2] = 0; pesos[3] = 0;
        }

        return pesos;
    }

    private void setupListeners() {
        // Listeners para todos os spinners de quantidade
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calcularPesos();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerQtdDianteiro1.setOnItemSelectedListener(listener);
        spinnerQtdDianteiro2.setOnItemSelectedListener(listener);
        spinnerQtdTraseiro1.setOnItemSelectedListener(listener);
        spinnerQtdTraseiro2.setOnItemSelectedListener(listener);
        spinnerQtdTraseiro3.setOnItemSelectedListener(listener);
        spinnerQtdTraseiro4.setOnItemSelectedListener(listener);
        spinnerCabine.setOnItemSelectedListener(listener);
    }

    private void setupButtons() {
        btnAvancar.setOnClickListener(v -> {
            salvarDadosERepassar();
        });

        btnRetornar.setOnClickListener(v -> finish());
    }

    private void calcularPesos() {
        try {
            String serie = dadosJardulli.getSerieTrator();

            // Busca pesos base
            int[] pesosDianteiros = getPesosDianteiros(serie);
            int[] pesosTraseiros = getPesosTraseiros(serie);

            // Calcula peso dianteiro
            int qtdD1 = (Integer) spinnerQtdDianteiro1.getSelectedItem();
            int qtdD2 = (Integer) spinnerQtdDianteiro2.getSelectedItem();
            pesoDianteiroCalculado = (qtdD1 * pesosDianteiros[0]) + (qtdD2 * pesosDianteiros[1]);

            // Calcula peso traseiro
            int qtdT1 = (Integer) spinnerQtdTraseiro1.getSelectedItem();
            int qtdT2 = (Integer) spinnerQtdTraseiro2.getSelectedItem();
            int qtdT3 = (Integer) spinnerQtdTraseiro3.getSelectedItem();
            int qtdT4 = (Integer) spinnerQtdTraseiro4.getSelectedItem();
            pesoTraseiroCalculado = (qtdT1 * pesosTraseiros[0]) +
                    (qtdT2 * pesosTraseiros[1]) +
                    (qtdT3 * pesosTraseiros[2]) +
                    (qtdT4 * pesosTraseiros[3]);

            // Calcula cabine
            int pesoCabine = spinnerCabine.getSelectedItem().toString().equals("Sim") ? 250 : 0;

            // Peso total
            pesoTotalCalculado = pesoDianteiroCalculado + pesoTraseiroCalculado + pesoCabine;

            // Atualiza interface
            textResultadoDianteiro.setText("Dianteiro: " + pesoDianteiroCalculado + " kg");
            textResultadoTraseiro.setText("Traseiro: " + pesoTraseiroCalculado + " kg");
            textPesoTotal.setText("Peso Total: " + pesoTotalCalculado + " kg");

            Log.d(TAG, "Dianteiro: " + pesoDianteiroCalculado + " kg");
            Log.d(TAG, "Traseiro: " + pesoTraseiroCalculado + " kg");
            Log.d(TAG, "Cabine: " + pesoCabine + " kg");
            Log.d(TAG, "Total: " + pesoTotalCalculado + " kg");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao calcular pesos", e);
            Toast.makeText(this, "Erro nos cálculos!", Toast.LENGTH_SHORT).show();
        }
    }

    private void salvarDadosERepassar() {
        try {
            String serie = dadosJardulli.getSerieTrator();

            // Busca os pesos reais da série
            int[] pesosTraseiros = getPesosTraseiros(serie);

            // Salva os dados para o relatório final
            dadosJardulli.setPesoFrontal(pesoDianteiroCalculado);
            dadosJardulli.setPesoRodados(pesoTraseiroCalculado);
            dadosJardulli.setLastroLiquido(pesoTotalCalculado);

            // ✅ CAPTURA TODAS AS 4 QUANTIDADES
            int qtdT1 = (Integer) spinnerQtdTraseiro1.getSelectedItem();
            int qtdT2 = (Integer) spinnerQtdTraseiro2.getSelectedItem();
            int qtdT3 = (Integer) spinnerQtdTraseiro3.getSelectedItem();
            int qtdT4 = (Integer) spinnerQtdTraseiro4.getSelectedItem();

            // ✅ SALVA AS QUANTIDADES CORRETAMENTE - TODOS OS 4 CAMPOS
            dadosJardulli.setQuantidadePesos91(qtdT1);     // Peso 1 (72kg/91kg)
            dadosJardulli.setQuantidadePesos227(qtdT2);    // Peso 2 (205kg/227kg)
            dadosJardulli.setQuantidadePesos454(qtdT3);    // Peso 3 (160kg/450kg/454kg)
            dadosJardulli.setQuantidadePesos625(qtdT4);    // Peso 4 (625kg)

            // ✅ CRÍTICO: Salvar os PESOS REAIS da série
            dadosJardulli.setPesosTraseirosReais(
                    pesosTraseiros[0],  // pesoRealTraseiro1 (ex: 72kg)
                    pesosTraseiros[1],  // pesoRealTraseiro2 (ex: 205kg)
                    pesosTraseiros[2],  // pesoRealTraseiro3 (ex: 160kg)
                    pesosTraseiros[3]   // pesoRealTraseiro4 (ex: 625kg)
            );

            // Salva strings para exibição
            String cabineStr = spinnerCabine.getSelectedItem().toString().equals("Sim") ? "Sim (250kg)" : "Não";

            dadosJardulli.setCabideStr(cabineStr);
            dadosJardulli.setPesoFrontalStr("Dianteiro: " + pesoDianteiroCalculado + " kg");
            dadosJardulli.setPesoTraseiro72Str("Traseiro: " + pesoTraseiroCalculado + " kg");
            dadosJardulli.setPesoTotal("Total: " + pesoTotalCalculado + " kg | Cabine: " + cabineStr);

            Log.d(TAG, "Dados salvos para relatório:");
            Log.d(TAG, "Peso Dianteiro: " + pesoDianteiroCalculado + " kg");
            Log.d(TAG, "Peso Traseiro: " + pesoTraseiroCalculado + " kg");
            Log.d(TAG, "Peso Total: " + pesoTotalCalculado + " kg");
            Log.d(TAG, "Cabine: " + cabineStr);
            Log.d(TAG, "Quantidades salvas - P1(72kg): " + qtdT1 + ", P2(205kg): " + qtdT2 +
                    ", P3(160kg): " + qtdT3 + ", P4(625kg): " + qtdT4);
            Log.d(TAG, "Pesos reais salvos - P1: " + pesosTraseiros[0] + "kg, P2: " + pesosTraseiros[1] +
                    "kg, P3: " + pesosTraseiros[2] + "kg, P4: " + pesosTraseiros[3] + "kg");

            // Navega para o relatório final
            Intent intent = new Intent(this, RelatorioFinalActivity.class);
            intent.putExtra("DADOS_JARDULLI", dadosJardulli);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao processar os dados!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Erro ao salvar dados", e);
        }
    }
}