package com.example.jardullicalculadora;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RelatorioFinalActivity extends AppCompatActivity {

    private static final String TAG = "RelatorioFinalActivity";
    private DadosJardulli dados;
    private double pesoTotal;
    private double pesoTotalDianteiro;
    private double pesoTOTALTraseiro;
    private double consumoDiesel;
    private PlanoManager planoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorio_final);

        dados = getIntent().getParcelableExtra("DADOS_JARDULLI");
        planoManager = new PlanoManager(this);
        initBotoes();

        if (dados != null) {
            exibirDadosEResultados();
            salvarBalancoNoBanco();
        } else {
            Toast.makeText(this, "Erro: Dados não recebidos!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initBotoes() {
        Button btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        Button btnGerarPDF = findViewById(R.id.btnGerarPDF);
        if (planoManager.podeGerarPDF()) {
            btnGerarPDF.setOnClickListener(v -> gerarPDF());
        } else {
            btnGerarPDF.setAlpha(0.5f);
            btnGerarPDF.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("PDF Bloqueado")
                        .setMessage("A geração de PDF está disponível nos planos Profissional e Frotista.")
                        .setPositiveButton("Ver Planos", (d, w) -> {
                            if (SupabaseClient.getInstance().isLoggedIn()) {
                                startActivity(new android.content.Intent(this, PlanosActivity.class));
                            } else {
                                startActivity(new android.content.Intent(this, AuthActivity.class));
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }
    }

    private void salvarBalancoNoBanco() {
        try {
            double divisaoDianteira = pesoTotal > 0 ? (pesoTotalDianteiro / pesoTotal) * 100 : 0;
            double divisaoTraseira = pesoTotal > 0 ? (pesoTOTALTraseiro / pesoTotal) * 100 : 0;
            double pesoPorCV = dados.getPesoTrator() > 0 ? pesoTotal / dados.getPesoTrator() : 0;
            double pesoPorKW = dados.getPotenciaKW() > 0 ? pesoTotal / dados.getPotenciaKW() : 0;

            DBHelper dbHelper = new DBHelper(this);
            dbHelper.salvarBalanco(dados, pesoTotal, pesoTotalDianteiro, pesoTOTALTraseiro,
                    divisaoDianteira, divisaoTraseira, pesoPorCV, pesoPorKW, consumoDiesel);
            planoManager.registrarBalanco();

            // Espelha o balanço na nuvem se o usuário está logado e o plano permite.
            // Falhas de upload não interrompem o fluxo (o registro local é a fonte de verdade local).
            SupabaseClient client = SupabaseClient.getInstance();
            if (client.isLoggedIn() && planoManager.podeVerHistorico()) {
                try {
                    org.json.JSONObject balanco = new org.json.JSONObject();
                    balanco.put("serie_trator", dados.getSerieTrator());
                    balanco.put("modelo", dados.getModelo());
                    balanco.put("operacao", dados.getOperacao());
                    balanco.put("ponto_engate", dados.getPontoEngate());
                    balanco.put("rodado_dianteiro", dados.getRodadoDianteiro());
                    balanco.put("rodado_traseiro", dados.getRodadoTraseiro());
                    balanco.put("peso_total", pesoTotal);
                    balanco.put("peso_dianteiro", pesoTotalDianteiro);
                    balanco.put("peso_traseiro", pesoTOTALTraseiro);
                    balanco.put("divisao_dianteira", divisaoDianteira);
                    balanco.put("divisao_traseira", divisaoTraseira);
                    balanco.put("peso_por_cv", pesoPorCV);
                    balanco.put("peso_por_kw", pesoPorKW);
                    balanco.put("consumo_diesel", consumoDiesel);

                    client.saveBalanco(balanco, new SupabaseClient.SupabaseCallback() {
                        @Override
                        public void onSuccess(org.json.JSONObject response) {
                            Log.d(TAG, "Balanço sincronizado na nuvem");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.w(TAG, "Falha ao sincronizar balanço na nuvem: " + errorMessage);
                        }
                    });
                } catch (Exception cloudEx) {
                    Log.w(TAG, "Erro ao montar payload do balanço para nuvem", cloudEx);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar balanço", e);
        }
    }

    private void gerarPDF() {
        File legacyFile = null;
        Uri mediaStoreUri = null;
        Document document = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Relatorio_Jardulli_" + timeStamp + ".pdf";

            OutputStream out;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage (API 29+): grava via MediaStore, que devolve uma
                // content:// URI pronta para uso — evita depender de File direto na
                // pasta pública Downloads, que é instável/bloqueada nessas versões.
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver resolver = getContentResolver();
                mediaStoreUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (mediaStoreUri == null) {
                    throw new IOException("Falha ao criar arquivo no MediaStore");
                }
                out = resolver.openOutputStream(mediaStoreUri);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                legacyFile = new File(downloadsDir, fileName);
                out = new FileOutputStream(legacyFile);
            }

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            document = new Document(pdfDoc);

            // CABEÇALHO
            Table headerTable = new Table(1);
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBackgroundColor(new DeviceRgb(0, 112, 60));

            Paragraph titulo = new Paragraph("JARDULLI\nRELATÓRIO DE CONFIGURAÇÃO")
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setBold()
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER);
            headerTable.addCell(new Cell().add(titulo).setBorder(null).setPadding(8));

            document.add(headerTable);
            // LINHA 72: TROCAR POR ESPAÇO MÍNIMO
            document.add(new Paragraph("").setMarginBottom(1));

            // CONFIGURAÇÃO DO TRATOR
            Paragraph secaoConfig = new Paragraph("CONFIGURAÇÃO DO TRATOR")
                    .setBold()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(secaoConfig);

            Table tableConfig = new Table(4);
            tableConfig.setWidth(UnitValue.createPercentValue(100));
            tableConfig.setMarginBottom(5); // Reduzido de 10 para 5

            adicionarCelulaCompacta(tableConfig, "Rodado Dianteiro", dados.getRodadoDianteiro(), true);
            adicionarCelulaCompacta(tableConfig, "Eixo Traseiro",
                    dados.getTipoEixoTraseiro() != null ? dados.getTipoEixoTraseiro() : "N/A", false);
            adicionarCelulaCompacta(tableConfig, "Rodado Traseiro", dados.getRodadoTraseiro(), true);
            adicionarCelulaCompacta(tableConfig, "Eixo Dianteiro",
                    dados.getTipoEixoDianteiro() != null ? dados.getTipoEixoDianteiro() : "N/A", false);

            adicionarCelulaCompacta(tableConfig, "Ponto de Engate", dados.getPontoEngate(), true);
            adicionarCelulaCompacta(tableConfig, "Potência CV", dados.getPesoTrator() + " CV", false);
            adicionarCelulaCompacta(tableConfig, "Operação", dados.getOperacao(), true);
            adicionarCelulaCompacta(tableConfig, "Peso Dianteiro", dados.getPesoFrontalStr(), false);

            adicionarCelulaCompacta(tableConfig, "Lastro Liq. Dianteiro", dados.getLastroDianteiro() + " kg", true);
            adicionarCelulaCompacta(tableConfig, "Lastro Liq. Traseiro", dados.getLastroTraseiro() + " kg", false);

            // PESOS TRASEIROS DINÂMICOS
            adicionarCelulaCompacta(tableConfig, "Pesos Traseiro " + dados.getPesoRealTraseiro1() + "kg",
                    dados.getQuantidadePesos91() + " un", true);
            adicionarCelulaCompacta(tableConfig, "Pesos Traseiro " + dados.getPesoRealTraseiro2() + "kg",
                    dados.getQuantidadePesos227() + " un", false);
            adicionarCelulaCompacta(tableConfig, "Pesos Traseiro " + dados.getPesoRealTraseiro3() + "kg",
                    dados.getQuantidadePesos454() + " un", true);
            adicionarCelulaCompacta(tableConfig, "Pesos Traseiro " + dados.getPesoRealTraseiro4() + "kg",
                    dados.getQuantidadePesos625() + " un", false);

            adicionarCelulaCompacta(tableConfig, "Peso Operador", dados.getPesoOperador() + " kg", true);
            adicionarCelulaCompacta(tableConfig, "Tanque Diesel", dados.getCapacidadeDiesel() + " L", false);
            // REMOVER AS LINHAS VAZIAS
            // adicionarCelulaCompacta(tableConfig, "", "", true);
            // adicionarCelulaCompacta(tableConfig, "", "", false);

            document.add(tableConfig);
            // LINHA 123: TROCAR POR ESPAÇO MÍNIMO
            document.add(new Paragraph("").setMarginBottom(1));

            // RESULTADOS E RECOMENDAÇÕES
            Table tableResultadosRecomendacoes = new Table(2);
            tableResultadosRecomendacoes.setWidth(UnitValue.createPercentValue(100));

            // RESULTADOS
            Cell cellResultados = new Cell();
            Paragraph tituloResultados = new Paragraph("RESULTADOS")
                    .setBold()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            cellResultados.add(tituloResultados);

            double pesoPorCVNominal = dados.getPesoTrator() > 0 ? pesoTotal / dados.getPesoTrator() : 0;
            double pesoPorKWReal = dados.getPotenciaKW() > 0 ? pesoTotal / dados.getPotenciaKW() : 0;
            double divisaoDianteira = pesoTotal > 0 ? (pesoTotalDianteiro / pesoTotal) * 100 : 0;
            double divisaoTraseira = pesoTotal > 0 ? (pesoTOTALTraseiro / pesoTotal) * 100 : 0;
            String divisaoEntreEixos = "N/A";
            if (dados.getDistanciaEntreEixos() > 0 && pesoTotal > 0) {
                double divisaoEixos = (pesoTotalDianteiro * dados.getDistanciaEntreEixos()) / pesoTotal;
                divisaoEntreEixos = format(divisaoEixos);
            }

            adicionarLinhaResultado(cellResultados, "PESO TOTAL:", formatSemDecimais(pesoTotal) + " kg");
            adicionarLinhaResultado(cellResultados, "PESO POR CV NOMINAL:", formatSemDecimais(pesoPorCVNominal) + " kg/CV");
            adicionarLinhaResultado(cellResultados, "PESO POR kW:", formatSemDecimais(pesoPorKWReal) + " kg/kW");
            adicionarLinhaResultado(cellResultados, "PESO NO EIXO DIANTEIRO:", formatSemDecimais(pesoTotalDianteiro) + " kg");
            adicionarLinhaResultado(cellResultados, "PESO NO EIXO TRASEIRO:", formatSemDecimais(pesoTOTALTraseiro) + " kg");
            adicionarLinhaResultado(cellResultados, "DIVISÃO DIANTEIRO:", formatSemDecimais(divisaoDianteira) + " %");
            adicionarLinhaResultado(cellResultados, "DIVISÃO TRASEIRO:", formatSemDecimais(divisaoTraseira) + " %");
            adicionarLinhaResultado(cellResultados, "DIV.ENTRE EIXO:", divisaoEntreEixos);
            adicionarLinhaResultado(cellResultados, "DIESEL L/h:", format(consumoDiesel));

            // RECOMENDAÇÕES
            Cell cellRecomendacoes = new Cell();
            Paragraph tituloRecomendacoes = new Paragraph("RECOMENDAÇÕES")
                    .setBold()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            cellRecomendacoes.add(tituloRecomendacoes);

            if (dados.getPesoMaximo() > 0) {
                String pontoEngate = dados.getPontoEngate();
                double divisaoDianteiraIdeal, divisaoTraseiraIdeal;

                if (pontoEngate != null) {
                    switch (pontoEngate.toUpperCase(Locale.ROOT)) {
                        case "3 PONTOS": divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0; break;
                        case "B.T.O.": divisaoDianteiraIdeal = 35.0; divisaoTraseiraIdeal = 65.0; break;
                        case "REBOQUE": divisaoDianteiraIdeal = 30.0; divisaoTraseiraIdeal = 70.0; break;
                        default: divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0;
                    }
                } else {
                    divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0;
                }

                double pesoDianteiroIdeal = (dados.getPesoMaximo() * divisaoDianteiraIdeal) / 100;
                double pesoTraseiroIdeal = (dados.getPesoMaximo() * divisaoTraseiraIdeal) / 100;

                adicionarLinhaResultado(cellRecomendacoes, "PESO MÁXIMO:", formatSemDecimais(dados.getPesoMaximo()) + " kg");

                if (dados.getPotenciaKW() > 0) {
                    double pesoPorKWRecomendado = dados.getPesoMaximo() / dados.getPotenciaKW();
                    adicionarLinhaResultado(cellRecomendacoes, "PESO POR kW RECOMENDADO:", formatSemDecimais(pesoPorKWRecomendado) + " kg/kW");
                }

                if (dados.getPesoTrator() > 0) {
                    double pesoPorCVRecomendado = dados.getPesoMaximo() / dados.getPesoTrator();
                    adicionarLinhaResultado(cellRecomendacoes, "PESO POR CV RECOMENDADO:", formatSemDecimais(pesoPorCVRecomendado) + " kg/CV");
                }

                adicionarLinhaResultado(cellRecomendacoes, "PESO DIANT. IDEAL:", formatSemDecimais(pesoDianteiroIdeal) + " kg");
                adicionarLinhaResultado(cellRecomendacoes, "PESO TRAS. IDEAL:", formatSemDecimais(pesoTraseiroIdeal) + " kg");
                adicionarLinhaResultado(cellRecomendacoes, "DIV. DIANT. IDEAL:", formatSemDecimais(divisaoDianteiraIdeal) + " %");
                adicionarLinhaResultado(cellRecomendacoes, "DIV. TRAS. IDEAL:", formatSemDecimais(divisaoTraseiraIdeal) + " %");
            } else {
                adicionarLinhaResultado(cellRecomendacoes, "PESO MÁXIMO:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "PESO POR kW RECOMENDADO:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "PESO POR CV RECOMENDADO:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "PESO DIANT. IDEAL:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "PESO TRAS. IDEAL:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "DIV. DIANT. IDEAL:", "N/A");
                adicionarLinhaResultado(cellRecomendacoes, "DIV. TRAS. IDEAL:", "N/A");
            }

            tableResultadosRecomendacoes.addCell(cellResultados);
            tableResultadosRecomendacoes.addCell(cellRecomendacoes);
            document.add(tableResultadosRecomendacoes);
            // LINHA 245: REMOVER COMPLETAMENTE
            // document.add(new Paragraph(" "));

            // ADICIONAR IMAGEM jartratir.jpg DO DRAWABLE COM LEGENDA NA PARTE INFERIOR
            try {
                // LINHA 251: REMOVER ESPAÇO ANTES DA IMAGEM
                // document.add(new Paragraph(" "));

                // Criar tabela para centralizar a imagem
                Table imageTable = new Table(1);
                imageTable.setWidth(UnitValue.createPercentValue(100));
                imageTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

                // Carregar imagem do drawable
                InputStream ims = null;
                byte[] imageData;
                try {
                    ims = getResources().openRawResource(R.raw.jartratir);
                    imageData = new byte[ims.available()];
                    ims.read(imageData);
                } finally {
                    if (ims != null) {
                        try { ims.close(); } catch (Exception ignored) { /* noop */ }
                    }
                }

                // Criar imagem do iText
                com.itextpdf.io.image.ImageData imageDataPdf =
                        com.itextpdf.io.image.ImageDataFactory.create(imageData);
                com.itextpdf.layout.element.Image pdfImage =
                        new com.itextpdf.layout.element.Image(imageDataPdf);

                // Ajustar tamanho da imagem para caber em uma página
                pdfImage.setWidth(UnitValue.createPercentValue(25)); // 25% da largura
                pdfImage.setAutoScale(true);
                pdfImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

                // Criar legenda com data/hora
                String dataGeracao = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                Paragraph legenda = new Paragraph("Gerado em: " + dataGeracao + " - Jardulli Calculadora")
                        .setFontColor(new DeviceRgb(255, 255, 255)) // Texto branco
                        .setFontSize(6) // Fonte pequena
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(new DeviceRgb(0, 112, 60)) // Fundo verde
                        .setPadding(2) // Pouco padding
                        .setMargin(0);

                // Adicionar imagem e legenda na mesma célula
                Cell imageCell = new Cell()
                        .add(pdfImage)
                        .add(legenda)
                        .setBorder(null)
                        .setPadding(0)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(new DeviceRgb(255, 255, 255));

                imageTable.addCell(imageCell);
                document.add(imageTable);
                // LINHA 293: REMOVER ESPAÇO APÓS IMAGEM
                // document.add(new Paragraph(" "));

            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar imagem: " + e.getMessage());
                // Se houver erro ao carregar a imagem, adicionar apenas a legenda
                String dataGeracao = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                Paragraph legendaFallback = new Paragraph("Gerado em: " + dataGeracao + " - Jardulli Calculadora")
                        .setFontColor(new DeviceRgb(0, 112, 60))
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(legendaFallback);
            }

            document.close();
            document = null;

            if (mediaStoreUri != null) {
                ContentValues doneValues = new ContentValues();
                doneValues.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(mediaStoreUri, doneValues, null, null);
                abrirPDF(mediaStoreUri, fileName);
            } else {
                abrirPDF(legacyFile);
            }
            Toast.makeText(this, "PDF gerado com sucesso!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            if (mediaStoreUri != null) {
                try {
                    getContentResolver().delete(mediaStoreUri, null, null);
                } catch (Exception cleanupEx) {
                    Log.w(TAG, "Falha ao limpar entrada pendente no MediaStore", cleanupEx);
                }
            }
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Erro ao gerar PDF", e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception closeEx) {
                    Log.w(TAG, "Falha ao fechar PDF document", closeEx);
                }
            }
        }
    }

    private void adicionarCelulaCompacta(Table table, String chave, String valor, boolean destacar) {
        Paragraph pChave = new Paragraph(chave).setFontSize(7).setBold();
        Paragraph pValor = new Paragraph(valor != null ? valor : "N/A").setFontSize(8);

        Cell cell = new Cell()
                .add(pChave)
                // LINHA 309: REMOVER PARÁGRAFO VAZIO ENTRE CHAVE E VALOR
                // .add(new Paragraph(" ")) // <--- REMOVIDO
                .add(pValor)
                .setPadding(2) // Reduzido de 3 para 2
                .setBorder(new SolidBorder(0.5f)); // Borda mais fina (0.5 em vez de 1)

        if (destacar) {
            cell.setBackgroundColor(new DeviceRgb(240, 240, 240));
        }
        table.addCell(cell);
    }

    private void adicionarLinhaResultado(Cell cell, String chave, String valor) {
        Table linha = new Table(new float[]{80, 20});
        linha.setWidth(UnitValue.createPercentValue(100));

        Paragraph pChave = new Paragraph(chave).setFontSize(7).setBold().setMargin(0).setPadding(0).setFixedLeading(8);
        Paragraph pValor = new Paragraph(valor).setFontSize(8).setMargin(0).setPadding(0).setFixedLeading(8);

        // LINHAS 330-331: REDUZIR ALTURA DE 10f PARA 9f
        linha.addCell(new Cell().add(pChave).setBorder(null).setPadding(0).setTextAlignment(TextAlignment.LEFT).setHeight(9f).setKeepTogether(true));
        linha.addCell(new Cell().add(pValor).setBorder(null).setPadding(0).setTextAlignment(TextAlignment.RIGHT).setHeight(9f).setKeepTogether(true));

        cell.add(linha);
    }

    private void abrirPDF(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            abrirPDF(uri, file.getName());
        } catch (Exception e) {
            Log.e(TAG, "Falha ao abrir PDF: " + file.getAbsolutePath(), e);
            Toast.makeText(this,
                    "PDF salvo em Downloads, mas não foi possível abrir automaticamente.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void abrirPDF(Uri uri, String fileName) {
        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            viewIntent.setClipData(ClipData.newUri(getContentResolver(), fileName, uri));

            if (abrirComChooser(viewIntent, uri, "Abrir PDF com")) {
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setClipData(ClipData.newUri(getContentResolver(), fileName, uri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

            if (abrirComChooser(shareIntent, uri, "Compartilhar PDF")) {
                Toast.makeText(this,
                        "PDF salvo. Nenhum leitor foi encontrado, então escolha um app para abrir ou compartilhar.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this,
                    "PDF salvo em Downloads, mas nenhum app compatível foi encontrado para abrir automaticamente.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Falha ao abrir PDF: " + uri, e);
            Toast.makeText(this,
                    "PDF salvo em Downloads, mas não foi possível abrir automaticamente.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean abrirComChooser(Intent baseIntent, Uri uri, String titulo) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> handlers = packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }

        for (ResolveInfo handler : handlers) {
            if (handler.activityInfo != null) {
                grantUriPermission(handler.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        startActivity(Intent.createChooser(baseIntent, titulo));
        return true;
    }

    private void exibirDadosEResultados() {
        setTextView(R.id.txtSerieTrator, "Série: " + dados.getSerieTrator());
        setTextView(R.id.txtModelo, "Modelo: " + dados.getTrator());
        setTextView(R.id.txtOperacao, "Operação: " + dados.getOperacao());
        setTextView(R.id.txtEngate, "Ponto de Engate: " + dados.getPontoEngate());
        setTextView(R.id.txtPotencia, "Potência: " + dados.getPesoTrator() + " CV");
        setTextView(R.id.txtPesoMaximo, "Peso Máximo: " + formatSemDecimais(dados.getPesoMaximo()) + " kg");

        setTextView(R.id.txtRodadoDianteiro, "Rodado Dianteiro: " + dados.getRodadoDianteiro());
        setTextView(R.id.txtRodadoTraseiro, "Rodado Traseiro: " + dados.getRodadoTraseiro());
        setTextView(R.id.txtEixoDianteiro, "Eixo Dianteiro: " + (dados.getTipoEixoDianteiro() != null ? dados.getTipoEixoDianteiro() : "N/A"));
        setTextView(R.id.txtEixoTraseiro, "Eixo Traseiro: " + (dados.getTipoEixoTraseiro() != null ? dados.getTipoEixoTraseiro() : "N/A"));

        setTextView(R.id.txtPesoDianteiro, "Peso Dianteiro: " + dados.getPesoFrontalStr());
        setTextView(R.id.txtLastroDianteiro, "Lastro Líquido Dianteiro: " + dados.getLastroDianteiro() + " kg");
        setTextView(R.id.txtLastroTraseiro, "Lastro Líquido Traseiro: " + dados.getLastroTraseiro() + " kg");

        // PESOS TRASEIROS DINÂMICOS
        setTextView(R.id.txtPesosTraseiro91, "Pesos Traseiro " + dados.getPesoRealTraseiro1() + "kg: " + dados.getQuantidadePesos91() + " unidades");
        setTextView(R.id.txtPesosTraseiro227, "Pesos Traseiro " + dados.getPesoRealTraseiro2() + "kg: " + dados.getQuantidadePesos227() + " unidades");
        setTextView(R.id.txtPesosTraseiro454, "Pesos Traseiro " + dados.getPesoRealTraseiro3() + "kg: " + dados.getQuantidadePesos454() + " unidades");
        setTextView(R.id.txtPesosTraseiro625, "Pesos Traseiro " + dados.getPesoRealTraseiro4() + "kg: " + dados.getQuantidadePesos625() + " unidades");

        setTextView(R.id.txtPesoOperador, "Peso Operador: " + dados.getPesoOperador() + " kg");
        setTextView(R.id.txtTanqueDiesel, "Tanque Diesel: " + dados.getCapacidadeDiesel() + " L");

        double[] resultados = calcularPesosCompletos(dados);
        pesoTotal = resultados[0];
        pesoTotalDianteiro = resultados[1];
        pesoTOTALTraseiro = resultados[2];

        setTextView(R.id.txtPesoTotalGeral, "PESO TOTAL: " + formatSemDecimais(pesoTotal) + " kg");

        if (dados.getPotenciaKW() > 0) {
            double pesoPorKWReal = pesoTotal / dados.getPotenciaKW();
            setTextView(R.id.txtPesoPorCVLiquido, "PESO POR kW: " + formatSemDecimais(pesoPorKWReal) + " kg/kW");
        } else {
            setTextView(R.id.txtPesoPorCVLiquido, "PESO POR kW: N/A");
        }

        setTextView(R.id.txtPesoPorCVNominal, "PESO POR CV NOMINAL: " + formatSemDecimais(pesoTotal / dados.getPesoTrator()) + " kg/CV");
        setTextView(R.id.txtPesoTotalDianteiro, "PESO NO EIXO DIANTEIRO: " + formatSemDecimais(pesoTotalDianteiro) + " kg");
        setTextView(R.id.txtPesoTOTALTraseiro, "PESO NO EIXO TRASEIRO: " + formatSemDecimais(pesoTOTALTraseiro) + " kg");

        calcularResultadosAdicionais(dados, pesoTotal, pesoTotalDianteiro, pesoTOTALTraseiro);
        consumoDiesel = calcularConsumoDiesel(dados);
        setTextView(R.id.txtDieselLh, "Diesel L/h: " + format(consumoDiesel));
    }

    private double[] calcularPesosCompletos(DadosJardulli dados) {
        double pesoTotal = 0;
        double pesoTotalDianteiro = 0;
        double pesoTOTALTraseiro = 0;

        // ✅ FÓRMULA SIMPLES: PESO_TOTAL = PESO_DIANTEIRO + PESO_TRASEIRO

        // 1. CALCULAR PESO DIANTEIRO TOTAL
        pesoTotalDianteiro =
                dados.getPesoEixoDianteiro() +                    // Peso do eixo dianteiro
                        dados.getLastroDianteiro() +                      // Lastro líquido dianteiro
                        dados.getPesoFrontal() +                          // Pesos frontais adicionais
                        (dados.getPesoOperador() > 0 ? dados.getPesoOperador() / 2.0 : 0) +  // Metade do operador
                        (dados.getCapacidadeDiesel() * 0.86 / 2.0);       // Metade do diesel

        // 2. CALCULAR PESO TRASEIRO TOTAL
        pesoTOTALTraseiro =
                dados.getPesoEixoTraseiro() +                     // Peso do eixo traseiro
                        dados.getLastroTraseiro() +                       // Lastro líquido traseiro
                        (dados.getPesoOperador() > 0 ? dados.getPesoOperador() / 2.0 : 0) +  // Metade do operador
                        (dados.getCapacidadeDiesel() * 0.86 / 2.0) +      // Metade do diesel
                        getPesoCabine(dados.getCabideStr()) +             // Cabine (100% traseiro)
                        (dados.getPesoImplemento() > 0 ? dados.getPesoImplemento() : 0) +  // Implemento (100% traseiro)
                        calcularPesosTraseirosAdicionais(dados);          // Pesos traseiros específicos

        // 3. PESO TOTAL = SOMA SIMPLES
        pesoTotal = pesoTotalDianteiro + pesoTOTALTraseiro;

        return new double[]{pesoTotal, pesoTotalDianteiro, pesoTOTALTraseiro};
    }

    private double calcularPesosTraseirosAdicionais(DadosJardulli dados) {
        double pesosTraseirosTotal = 0;

        // Peso 1 (72kg/91kg) - usa quantidadePesos91
        if (dados.getPesoRealTraseiro1() > 0) {
            pesosTraseirosTotal += dados.getQuantidadePesos91() * dados.getPesoRealTraseiro1();
        }

        // Peso 2 (205kg/227kg) - usa quantidadePesos227
        if (dados.getPesoRealTraseiro2() > 0) {
            pesosTraseirosTotal += dados.getQuantidadePesos227() * dados.getPesoRealTraseiro2();
        }

        // Peso 3 (160kg/450kg/454kg) - usa quantidadePesos454
        if (dados.getPesoRealTraseiro3() > 0) {
            pesosTraseirosTotal += dados.getQuantidadePesos454() * dados.getPesoRealTraseiro3();
        }

        // Peso 4 (625kg) - usa quantidadePesos625
        if (dados.getPesoRealTraseiro4() > 0) {
            pesosTraseirosTotal += dados.getQuantidadePesos625() * dados.getPesoRealTraseiro4();
        }

        return pesosTraseirosTotal;
    }

    private double getPesoCabine(String cabideStr) {
        if (cabideStr == null) return 0;
        String cabine = cabideStr.trim();
        if (cabine.isEmpty()) return 0;
        if (cabine.equalsIgnoreCase("Sim") || cabine.equalsIgnoreCase("Sim (250kg)") || cabine.contains("250")) {
            return 250.0;
        }
        return 0;
    }

    private double calcularConsumoDiesel(DadosJardulli dados) {
        double potenciaCV = dados.getPesoTrator();
        String operacao = dados.getOperacao();
        double fatorOperacao;
        switch (operacao.toUpperCase(Locale.ROOT)) {
            case "LEVE": fatorOperacao = 0.45; break;
            case "MÉDIO": fatorOperacao = 0.55; break;
            case "PESADO": fatorOperacao = 0.75; break;
            default: fatorOperacao = 0.55;
        }
        return potenciaCV * fatorOperacao * 0.15;
    }

    private void calcularResultadosAdicionais(DadosJardulli dados, double pesoTotal, double pesoTotalDianteiro, double pesoTOTALTraseiro) {
        try {
            if (dados.getPesoTrator() > 0) {
                double pesoPorCVNominal = pesoTotal / dados.getPesoTrator();
                setTextView(R.id.txtPesoPorCVNominal, "PESO POR CV NOMINAL: " + formatSemDecimais(pesoPorCVNominal) + " kg/CV");
            }

            double divisaoDianteira = (pesoTotalDianteiro / pesoTotal) * 100;
            double divisaoTraseira = (pesoTOTALTraseiro / pesoTotal) * 100;

            setTextView(R.id.txtDivisaoDianteiro, "DIVISÃO DIANTEIRO: " + formatSemDecimais(divisaoDianteira) + " %");
            setTextView(R.id.txtDivisaoTraseiro, "DIVISÃO TRASEIRO: " + formatSemDecimais(divisaoTraseira) + " %");

            if (dados.getDistanciaEntreEixos() > 0 && pesoTotal > 0) {
                double divisaoEntreEixos = (pesoTotalDianteiro * dados.getDistanciaEntreEixos()) / pesoTotal;
                setTextView(R.id.txtDivisaoEntreEixos, "DIVISÃO ENTRE EIXO: " + format(divisaoEntreEixos));
            } else {
                setTextView(R.id.txtDivisaoEntreEixos, "DIVISÃO ENTRE EIXO: N/A");
            }

            calcularPesosIdeais(dados);
            calcularRecomendacoes(dados);

        } catch (Exception e) {
            // Silencioso
        }
    }

    private void calcularPesosIdeais(DadosJardulli dados) {
        if (dados.getPesoMaximo() <= 0) return;
        String pontoEngate = dados.getPontoEngate();
        double divisaoDianteiraIdeal, divisaoTraseiraIdeal;

        if (pontoEngate != null) {
            switch (pontoEngate.toUpperCase()) {
                case "3 PONTOS": divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0; break;
                case "B.T.O.": divisaoDianteiraIdeal = 35.0; divisaoTraseiraIdeal = 65.0; break;
                case "REBOQUE": divisaoDianteiraIdeal = 30.0; divisaoTraseiraIdeal = 70.0; break;
                default: divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0;
            }
        } else {
            divisaoDianteiraIdeal = 40.0; divisaoTraseiraIdeal = 60.0;
        }

        double pesoDianteiroIdeal = (dados.getPesoMaximo() * divisaoDianteiraIdeal) / 100;
        double pesoTraseiroIdeal = (dados.getPesoMaximo() * divisaoTraseiraIdeal) / 100;

        setTextView(R.id.txtPesoEixoDianteiroIdeal, "Peso Dianteiro Ideal: " + formatSemDecimais(pesoDianteiroIdeal) + " kg");
        setTextView(R.id.txtPesoEixoTraseiroIdeal, "Peso Traseiro Ideal: " + formatSemDecimais(pesoTraseiroIdeal) + " kg");
        setTextView(R.id.txtDivisaoDianteiraIdeal1, "Divisão Dianteira Ideal: " + formatSemDecimais(divisaoDianteiraIdeal) + " %");
        setTextView(R.id.txtDivisaoDianteiraIdeal2, "Divisão Traseira Ideal: " + formatSemDecimais(divisaoTraseiraIdeal) + " %");
    }

    private void calcularRecomendacoes(DadosJardulli dados) {
        try {
            if (dados.getPotenciaKW() > 0 && dados.getPesoMaximo() > 0) {
                double pesoPorKWRecomendado = dados.getPesoMaximo() / dados.getPotenciaKW();
                setTextView(R.id.txtRecomendacaoKWNominal, "Peso por kW Recomendado: " + formatSemDecimais(pesoPorKWRecomendado) + " kg/kW");
            }

            if (dados.getPesoTrator() > 0 && dados.getPesoMaximo() > 0) {
                double pesoPorCVRecomendado = dados.getPesoMaximo() / dados.getPesoTrator();
                setTextView(R.id.txtRecomendacaoCVNominal, "Peso por CV Recomendado: " + formatSemDecimais(pesoPorCVRecomendado) + " kg/CV");
            }
        } catch (Exception e) {
            // Silencioso
        }
    }

    private void setTextView(int id, String text) {
        TextView textView = findViewById(id);
        if (textView != null) {
            textView.setText(text);
        }
    }

    private String formatSemDecimais(double valor) {
        return String.format(Locale.ROOT, "%,.0f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String format(double valor) {
        return String.format(Locale.ROOT, "%,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}