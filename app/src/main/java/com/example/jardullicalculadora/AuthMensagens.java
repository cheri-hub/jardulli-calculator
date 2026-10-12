package com.example.jardullicalculadora;

import java.util.Locale;

/**
 * Traduz as mensagens de erro do Supabase Auth (em inglês) para PT-BR.
 * Sem dependências de Android — testável por JUnit puro.
 */
public final class AuthMensagens {

    private AuthMensagens() {}

    /** true se o erro é o limite de envio de e-mails do Supabase (o pedido anterior ainda vale). */
    public static boolean ehLimiteDeEnvio(String erro) {
        if (erro == null) return false;
        String m = erro.toLowerCase(Locale.ROOT);
        return m.contains("rate limit") || m.contains("you can only request this after")
                || m.contains("rate_limit");
    }

    public static String traduzir(String erro) {
        if (erro == null || erro.trim().isEmpty()) return "Erro desconhecido";

        String m = erro.toLowerCase(Locale.ROOT);

        if (m.startsWith("falha na conexão")) {
            return "Sem conexão. Verifique a internet e tente de novo.";
        }
        if (m.contains("expired or is invalid") || m.contains("otp_expired")) {
            return "Código inválido ou expirado. Peça um novo código.";
        }
        if (m.contains("rate limit") || m.contains("you can only request this after")) {
            return "Aguarde cerca de um minuto para pedir um novo código.";
        }
        if (m.contains("different from the old password")) {
            return "A nova senha deve ser diferente da anterior.";
        }
        if (m.contains("password should be at least")) {
            return "A senha deve ter pelo menos " + SenhaValidator.TAMANHO_MINIMO_SENHA + " caracteres";
        }
        return erro;
    }
}
