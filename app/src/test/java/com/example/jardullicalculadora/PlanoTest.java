package com.example.jardullicalculadora;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlanoTest {

    @Test
    public void testdriveNaoGeraPdf() {
        assertFalse(Plano.TESTDRIVE.isPodePDF());
    }

    @Test
    public void planosPagosGeramPdf() {
        assertTrue(Plano.FLEXIVEL.isPodePDF());
        assertTrue(Plano.PROFISSIONAL.isPodePDF());
        assertTrue(Plano.FROTISTA.isPodePDF());
    }

    @Test
    public void historicoSoParaPlanosPagos() {
        assertFalse(Plano.TESTDRIVE.isPodeHistorico());
        assertTrue(Plano.FLEXIVEL.isPodeHistorico());
    }
}
