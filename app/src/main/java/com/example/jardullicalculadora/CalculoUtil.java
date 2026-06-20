package com.example.jardullicalculadora;

import java.util.Locale;

/**
 * Funções puras de cálculo de balanço — extraídas de
 * {@link RelatorioFinalActivity} para permitir testes unitários
 * (JUnit puro, sem instrumentação Android).
 */
public final class CalculoUtil {

    /** Fator de operação para consumo de diesel (L/h por CV). */
    public static final double FATOR_LEVE = 0.45;
    public static final double FATOR_MEDIO = 0.55;
    public static final double FATOR_PESADO = 0.75;
    public static final double CONSTANTE_DIESEL = 0.15;

    private CalculoUtil() {}

    /** Divisão segura: retorna 0 se denominador for &lt;= 0. */
    public static double divisaoSegura(double numerador, double denominador) {
        if (denominador <= 0) return 0;
        return numerador / denominador;
    }

    /** Percentual seguro: {@code (parte / total) * 100}, ou 0 se total &lt;= 0. */
    public static double percentualSeguro(double parte, double total) {
        return divisaoSegura(parte, total) * 100;
    }

    /**
     * Consumo aproximado de diesel em litros por hora.
     *
     * @param potenciaCV potência nominal do trator
     * @param fatorOperacao 0.45 (LEVE), 0.55 (MÉDIO) ou 0.75 (PESADO)
     */
    public static double consumoDiesel(double potenciaCV, double fatorOperacao) {
        if (potenciaCV <= 0 || fatorOperacao <= 0) return 0;
        return potenciaCV * fatorOperacao * CONSTANTE_DIESEL;
    }

    /**
     * Distribuição ideal frontal x traseiro por ponto de engate.
     *
     * @param pontoEngate "3 PONTOS", "B.T.O.", "REBOQUE" (case-insensitive)
     * @return array {dianteiraPercent, traseiraPercent}; default 40/60
     */
    public static double[] divisaoIdealPorEngate(String pontoEngate) {
        if (pontoEngate == null) return new double[]{40.0, 60.0};
        switch (pontoEngate.toUpperCase(Locale.ROOT)) {
            case "3 PONTOS": return new double[]{40.0, 60.0};
            case "B.T.O.":   return new double[]{35.0, 65.0};
            case "REBOQUE":  return new double[]{30.0, 70.0};
            default:         return new double[]{40.0, 60.0};
        }
    }

    /** Peso ideal em kg para um eixo, dado peso máximo e percentual. */
    public static double pesoIdealEixo(double pesoMaximo, double percentual) {
        if (pesoMaximo <= 0) return 0;
        return (pesoMaximo * percentual) / 100;
    }
}
