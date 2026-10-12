package com.example.jardullicalculadora;

import java.util.regex.Pattern;

/**
 * Validações puras da recuperação de senha (e-mail, código e nova senha).
 * Sem dependências de Android — testável por JUnit puro.
 *
 * Cada método devolve {@code null} quando o valor é válido ou a mensagem de
 * erro (em PT-BR) pronta para mostrar ao usuário.
 */
public final class SenhaValidator {

    /** Mesmo mínimo exigido no cadastro (AuthActivity). */
    public static final int TAMANHO_MINIMO_SENHA = 6;
    public static final int TAMANHO_CODIGO = 6;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CODIGO = Pattern.compile("^\\d{" + TAMANHO_CODIGO + "}$");

    private SenhaValidator() {}

    public static String validarEmail(String email) {
        String valor = email == null ? "" : email.trim();
        if (valor.isEmpty()) return "Informe o e-mail";
        if (!EMAIL.matcher(valor).matches()) return "E-mail inválido";
        return null;
    }

    public static String validarCodigo(String codigo) {
        String valor = codigo == null ? "" : codigo.trim();
        if (!CODIGO.matcher(valor).matches()) {
            return "Informe o código de " + TAMANHO_CODIGO + " dígitos";
        }
        return null;
    }

    public static String validarNovaSenha(String senha, String confirmacao) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            return "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres";
        }
        if (!senha.equals(confirmacao)) return "As senhas não conferem";
        return null;
    }
}
