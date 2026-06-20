package com.example.jardullicalculadora;

/**
 * Validador puro de CPF e CNPJ pelo algoritmo de dígitos verificadores
 * (Módulo 11). Sem dependências de Android — testável por JUnit puro.
 *
 * Uso:
 *   if (DocumentValidator.isValid("123.456.789-09")) { ... }
 */
public final class DocumentValidator {

    private DocumentValidator() {}

    /** Aceita CPF ou CNPJ (com ou sem máscara). */
    public static boolean isValid(String doc) {
        if (doc == null) return false;
        String numeros = doc.replaceAll("[^0-9]", "");
        if (numeros.length() == 11) return isValidCpf(numeros);
        if (numeros.length() == 14) return isValidCnpj(numeros);
        return false;
    }

    public static boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return false;
        if (cpf.matches("(\\d)\\1{10}")) return false;
        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (cpf.charAt(i) - '0') * (10 - i);
            }
            int dig1 = 11 - (soma % 11);
            if (dig1 >= 10) dig1 = 0;
            if (dig1 != cpf.charAt(9) - '0') return false;

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (cpf.charAt(i) - '0') * (11 - i);
            }
            int dig2 = 11 - (soma % 11);
            if (dig2 >= 10) dig2 = 0;
            return dig2 == cpf.charAt(10) - '0';
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return false;
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        try {
            int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += (cnpj.charAt(i) - '0') * peso1[i];
            }
            int dig1 = soma % 11;
            dig1 = (dig1 < 2) ? 0 : 11 - dig1;
            if (dig1 != cnpj.charAt(12) - '0') return false;

            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += (cnpj.charAt(i) - '0') * peso2[i];
            }
            int dig2 = soma % 11;
            dig2 = (dig2 < 2) ? 0 : 11 - dig2;
            return dig2 == cnpj.charAt(13) - '0';
        } catch (Exception e) {
            return false;
        }
    }
}
