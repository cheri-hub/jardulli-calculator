package com.example.jardullicalculadora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthMensagensTest {

    @Test
    public void codigoExpiradoOuInvalido() {
        String esperado = "Código inválido ou expirado. Peça um novo código.";
        assertEquals(esperado, AuthMensagens.traduzir("Token has expired or is invalid"));
        assertEquals(esperado, AuthMensagens.traduzir("otp_expired: Token has expired or is invalid"));
    }

    @Test
    public void limiteDeEnvio() {
        String esperado = "Aguarde cerca de um minuto para pedir um novo código.";
        assertEquals(esperado, AuthMensagens.traduzir(
                "For security purposes, you can only request this after 56 seconds."));
        assertEquals(esperado, AuthMensagens.traduzir("Email rate limit exceeded"));
    }

    @Test
    public void ehLimiteDeEnvio_reconheceOsErrosDeLimite() {
        assertTrue(AuthMensagens.ehLimiteDeEnvio(
                "For security purposes, you can only request this after 56 seconds."));
        assertTrue(AuthMensagens.ehLimiteDeEnvio("Email rate limit exceeded"));
        assertTrue(AuthMensagens.ehLimiteDeEnvio("over_email_send_rate_limit: slow down"));
    }

    @Test
    public void ehLimiteDeEnvio_naoConfundeComOutrosErros() {
        assertFalse(AuthMensagens.ehLimiteDeEnvio("Token has expired or is invalid"));
        assertFalse(AuthMensagens.ehLimiteDeEnvio("Falha na conexão: timeout"));
        assertFalse(AuthMensagens.ehLimiteDeEnvio(null));
        assertFalse(AuthMensagens.ehLimiteDeEnvio(""));
    }

    @Test
    public void novaSenhaIgualAAnterior() {
        assertEquals("A nova senha deve ser diferente da anterior.",
                AuthMensagens.traduzir("New password should be different from the old password."));
    }

    @Test
    public void senhaFraca() {
        assertEquals("A senha deve ter pelo menos 6 caracteres",
                AuthMensagens.traduzir("Password should be at least 6 characters."));
    }

    @Test
    public void semConexao() {
        assertEquals("Sem conexão. Verifique a internet e tente de novo.",
                AuthMensagens.traduzir("Falha na conexão: Unable to resolve host"));
    }

    @Test
    public void ignoraMaiusculasEMinusculas() {
        assertEquals("Código inválido ou expirado. Peça um novo código.",
                AuthMensagens.traduzir("TOKEN HAS EXPIRED OR IS INVALID"));
    }

    @Test
    public void desconhecidoPassaDireto_eNuloViraErroGenerico() {
        assertEquals("Algo inesperado", AuthMensagens.traduzir("Algo inesperado"));
        assertEquals("Erro desconhecido", AuthMensagens.traduzir(null));
        assertEquals("Erro desconhecido", AuthMensagens.traduzir(""));
    }
}
