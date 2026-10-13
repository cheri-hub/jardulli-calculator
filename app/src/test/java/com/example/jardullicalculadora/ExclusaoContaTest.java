package com.example.jardullicalculadora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExclusaoContaTest {

    @Test
    public void confirmacao_aceitaExcluirIgnorandoCaixaEEspacos() {
        assertTrue(ExclusaoConta.confirmacaoValida("EXCLUIR"));
        assertTrue(ExclusaoConta.confirmacaoValida("excluir"));
        assertTrue(ExclusaoConta.confirmacaoValida("  Excluir  "));
    }

    @Test
    public void confirmacao_rejeitaQualquerOutraCoisa() {
        assertFalse(ExclusaoConta.confirmacaoValida(null));
        assertFalse(ExclusaoConta.confirmacaoValida(""));
        assertFalse(ExclusaoConta.confirmacaoValida("EXCLUI"));
        assertFalse(ExclusaoConta.confirmacaoValida("EXCLUIR CONTA"));
        assertFalse(ExclusaoConta.confirmacaoValida("sim"));
    }

    @Test
    public void erroDoGateway_dizQueNadaFoiExcluido() {
        assertEquals("Não foi possível cancelar a assinatura agora. Nada foi excluído. "
                + "Tente de novo em instantes.", ExclusaoConta.mensagemDeErro("Payment gateway error"));
    }

    @Test
    public void semConexao() {
        assertEquals("Sem conexão. Verifique a internet e tente de novo. Nada foi excluído.",
                ExclusaoConta.mensagemDeErro("Falha na conexão: Unable to resolve host"));
    }

    @Test
    public void limiteDeTentativas() {
        assertEquals("Muitas tentativas. Aguarde um minuto e tente de novo.",
                ExclusaoConta.mensagemDeErro("Too many requests. Try again in a minute."));
    }

    @Test
    public void sessaoExpirada_naoAfirmaQueNadaFoiExcluido() {
        // Pode ser a nova tentativa depois de uma exclusão cuja resposta se perdeu.
        String msg = ExclusaoConta.mensagemDeErro("Sessão expirada. Faça login novamente.");
        assertEquals("Sua sessão expirou. Entre novamente: se a conta já tiver sido excluída, "
                + "o login não vai funcionar.", msg);
        assertFalse(msg.contains("Nada foi excluído"));
    }

    @Test
    public void erroDesconhecidoOuNulo_naoVazaDetalheTecnico() {
        String esperado = "Não foi possível excluir a conta. Nada foi excluído. Tente de novo.";
        assertEquals(esperado, ExclusaoConta.mensagemDeErro("Internal server error"));
        assertEquals(esperado, ExclusaoConta.mensagemDeErro(null));
        assertEquals(esperado, ExclusaoConta.mensagemDeErro(""));
    }
}
