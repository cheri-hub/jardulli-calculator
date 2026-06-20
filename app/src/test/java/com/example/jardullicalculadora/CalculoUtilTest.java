package com.example.jardullicalculadora;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalculoUtilTest {

    private static final double DELTA = 0.001;

    // ──────────── divisaoSegura ────────────
    @Test
    public void divisaoSegura_normal() {
        assertEquals(2.0, CalculoUtil.divisaoSegura(10, 5), DELTA);
    }

    @Test
    public void divisaoSegura_denominadorZero() {
        assertEquals(0.0, CalculoUtil.divisaoSegura(10, 0), DELTA);
    }

    @Test
    public void divisaoSegura_denominadorNegativo() {
        assertEquals(0.0, CalculoUtil.divisaoSegura(10, -3), DELTA);
    }

    // ──────────── percentualSeguro ────────────
    @Test
    public void percentual_metade() {
        assertEquals(50.0, CalculoUtil.percentualSeguro(50, 100), DELTA);
    }

    @Test
    public void percentual_totalZero_naoExplode() {
        assertEquals(0.0, CalculoUtil.percentualSeguro(50, 0), DELTA);
    }

    // ──────────── consumoDiesel ────────────
    @Test
    public void consumoDiesel_leve_100cv() {
        // 100 * 0.45 * 0.15 = 6.75 L/h
        assertEquals(6.75, CalculoUtil.consumoDiesel(100, CalculoUtil.FATOR_LEVE), DELTA);
    }

    @Test
    public void consumoDiesel_pesado_200cv() {
        // 200 * 0.75 * 0.15 = 22.5
        assertEquals(22.5, CalculoUtil.consumoDiesel(200, CalculoUtil.FATOR_PESADO), DELTA);
    }

    @Test
    public void consumoDiesel_potenciaZero() {
        assertEquals(0.0, CalculoUtil.consumoDiesel(0, CalculoUtil.FATOR_LEVE), DELTA);
    }

    // ──────────── divisaoIdealPorEngate ────────────
    @Test
    public void engate_3pontos_40_60() {
        assertArrayEquals(new double[]{40, 60}, CalculoUtil.divisaoIdealPorEngate("3 PONTOS"), DELTA);
    }

    @Test
    public void engate_bto_35_65() {
        assertArrayEquals(new double[]{35, 65}, CalculoUtil.divisaoIdealPorEngate("B.T.O."), DELTA);
        // case-insensitive
        assertArrayEquals(new double[]{35, 65}, CalculoUtil.divisaoIdealPorEngate("b.t.o."), DELTA);
    }

    @Test
    public void engate_reboque_30_70() {
        assertArrayEquals(new double[]{30, 70}, CalculoUtil.divisaoIdealPorEngate("REBOQUE"), DELTA);
    }

    @Test
    public void engate_null_default40_60() {
        assertArrayEquals(new double[]{40, 60}, CalculoUtil.divisaoIdealPorEngate(null), DELTA);
    }

    @Test
    public void engate_desconhecido_default40_60() {
        assertArrayEquals(new double[]{40, 60}, CalculoUtil.divisaoIdealPorEngate("XYZ"), DELTA);
    }

    // ──────────── pesoIdealEixo ────────────
    @Test
    public void pesoIdeal_metade() {
        // 10000 kg * 50% = 5000
        assertEquals(5000.0, CalculoUtil.pesoIdealEixo(10000, 50), DELTA);
    }

    @Test
    public void pesoIdeal_pesoMaximoZero() {
        assertEquals(0.0, CalculoUtil.pesoIdealEixo(0, 50), DELTA);
    }
}
