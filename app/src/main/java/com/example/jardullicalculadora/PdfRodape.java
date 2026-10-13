package com.example.jardullicalculadora;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

/** Rodapé do relatório em PDF. Sem dependência de Android — testável por JUnit puro. */
public final class PdfRodape {

    private static final float DISTANCIA_DA_BORDA = 18f;

    private PdfRodape() {}

    /**
     * Escreve o texto centralizado no pé de cada página. Usa posição absoluta
     * (showTextAligned), então não consome espaço do fluxo nem cria página extra.
     * Chamar depois de todo o conteúdo e antes de {@code document.close()}.
     */
    public static void adicionar(Document document, PdfDocument pdfDoc, String texto) {
        float centro = pdfDoc.getDefaultPageSize().getWidth() / 2f;
        for (int pagina = 1; pagina <= pdfDoc.getNumberOfPages(); pagina++) {
            Paragraph rodape = new Paragraph(texto)
                    .setFontColor(new DeviceRgb(117, 117, 117))
                    .setFontSize(8);
            document.showTextAligned(rodape, centro, DISTANCIA_DA_BORDA, pagina,
                    TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0);
        }
    }
}
