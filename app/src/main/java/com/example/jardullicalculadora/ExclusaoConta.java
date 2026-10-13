package com.example.jardullicalculadora;

import java.util.Locale;

/**
 * Regras puras da exclusão de conta (confirmação digitada e mensagens de erro).
 * Sem dependências de Android — testável por JUnit puro.
 */
public final class ExclusaoConta {

    public static final String PALAVRA_DE_CONFIRMACAO = "EXCLUIR";

    private ExclusaoConta() {}

    /** O usuário precisa digitar a palavra de confirmação (sem distinguir caixa nem espaços nas pontas). */
    public static boolean confirmacaoValida(String digitado) {
        return digitado != null
                && PALAVRA_DE_CONFIRMACAO.equals(digitado.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Traduz o erro da Edge Function. Toda mensagem deixa claro que nada foi
     * excluído, e detalhes técnicos do servidor nunca chegam à tela.
     */
    public static String mensagemDeErro(String erro) {
        String m = erro == null ? "" : erro.toLowerCase(Locale.ROOT);

        if (m.startsWith("falha na conexão")) {
            return "Sem conexão. Verifique a internet e tente de novo. Nada foi excluído.";
        }
        if (m.contains("payment gateway")) {
            return "Não foi possível cancelar a assinatura agora. Nada foi excluído. "
                    + "Tente de novo em instantes.";
        }
        if (m.startsWith("sessão expirada")) {
            // Pode ser a nova tentativa depois de uma exclusão cuja resposta se perdeu:
            // não dá para afirmar que nada foi excluído.
            return "Sua sessão expirou. Entre novamente: se a conta já tiver sido excluída, "
                    + "o login não vai funcionar.";
        }
        if (m.contains("too many requests")) {
            return "Muitas tentativas. Aguarde um minuto e tente de novo.";
        }
        return "Não foi possível excluir a conta. Nada foi excluído. Tente de novo.";
    }
}
