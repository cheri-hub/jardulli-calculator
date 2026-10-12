package com.example.jardullicalculadora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SenhaValidatorTest {

    // ──────────── E-mail ────────────
    @Test
    public void email_vazioOuNulo_pedeEmail() {
        assertEquals("Informe o e-mail", SenhaValidator.validarEmail(null));
        assertEquals("Informe o e-mail", SenhaValidator.validarEmail(""));
        assertEquals("Informe o e-mail", SenhaValidator.validarEmail("   "));
    }

    @Test
    public void email_semFormato_invalido() {
        assertEquals("E-mail inválido", SenhaValidator.validarEmail("abc"));
        assertEquals("E-mail inválido", SenhaValidator.validarEmail("a@b"));
        assertEquals("E-mail inválido", SenhaValidator.validarEmail("a b@c.com"));
    }

    @Test
    public void email_valido_aceitoMesmoComEspacosNasPontas() {
        assertNull(SenhaValidator.validarEmail("joao@exemplo.com"));
        assertNull(SenhaValidator.validarEmail("  joao@exemplo.com.br "));
    }

    // ──────────── Código ────────────
    @Test
    public void codigo_nuloOuVazio_pedeCodigo() {
        assertNotNull(SenhaValidator.validarCodigo(null));
        assertNotNull(SenhaValidator.validarCodigo(""));
    }

    @Test
    public void codigo_tamanhoErradoOuComLetras_invalido() {
        assertNotNull(SenhaValidator.validarCodigo("12345"));
        assertNotNull(SenhaValidator.validarCodigo("1234567"));
        assertNotNull(SenhaValidator.validarCodigo("12a456"));
    }

    @Test
    public void codigo_seisDigitos_aceito() {
        assertNull(SenhaValidator.validarCodigo("123456"));
        assertNull(SenhaValidator.validarCodigo(" 123456 "));
        assertNull(SenhaValidator.validarCodigo("000000"));
    }

    // ──────────── Nova senha ────────────
    @Test
    public void senha_curta_recusada() {
        assertEquals("A senha deve ter pelo menos 6 caracteres",
                SenhaValidator.validarNovaSenha("12345", "12345"));
        assertEquals("A senha deve ter pelo menos 6 caracteres",
                SenhaValidator.validarNovaSenha(null, null));
    }

    @Test
    public void senha_confirmacaoDiferente_recusada() {
        assertEquals("As senhas não conferem",
                SenhaValidator.validarNovaSenha("abcdef", "abcdeg"));
        assertEquals("As senhas não conferem",
                SenhaValidator.validarNovaSenha("abcdef", null));
    }

    @Test
    public void senha_validaEIgual_aceita() {
        assertNull(SenhaValidator.validarNovaSenha("abcdef", "abcdef"));
        assertNull(SenhaValidator.validarNovaSenha("Senh@ Longa 123", "Senh@ Longa 123"));
    }

    @Test
    public void senha_comprimentoTemPrioridadeSobreConfirmacao() {
        assertEquals("A senha deve ter pelo menos 6 caracteres",
                SenhaValidator.validarNovaSenha("abc", "xyz"));
    }
}
