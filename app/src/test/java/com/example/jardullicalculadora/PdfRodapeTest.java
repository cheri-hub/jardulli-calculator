package com.example.jardullicalculadora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class PdfRodapeTest {

    private static final String TEXTO = "Powered by Agrosinergia";

    private byte[] gerar(int paginas) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdf);
        for (int i = 1; i <= paginas; i++) {
            doc.add(new Paragraph("Conteudo da pagina " + i));
            if (i < paginas) doc.add(new AreaBreak());
        }
        PdfRodape.adicionar(doc, pdf, TEXTO);
        doc.close();
        return out.toByteArray();
    }

    @Test
    public void escreveORodapeEmCadaPagina_semCriarPaginaExtra() throws Exception {
        PdfDocument lido = new PdfDocument(new PdfReader(new ByteArrayInputStream(gerar(2))));
        assertEquals(2, lido.getNumberOfPages());
        for (int p = 1; p <= 2; p++) {
            String texto = PdfTextExtractor.getTextFromPage(lido.getPage(p));
            assertTrue("pagina " + p, texto.contains(TEXTO));
            assertTrue(texto.contains("Conteudo da pagina " + p));
        }
        lido.close();
    }

    @Test
    public void umaPagina() throws Exception {
        PdfDocument lido = new PdfDocument(new PdfReader(new ByteArrayInputStream(gerar(1))));
        assertEquals(1, lido.getNumberOfPages());
        assertTrue(PdfTextExtractor.getTextFromPage(lido.getPage(1)).contains(TEXTO));
        lido.close();
    }
}
