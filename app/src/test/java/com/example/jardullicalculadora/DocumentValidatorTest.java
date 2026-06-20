package com.example.jardullicalculadora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DocumentValidatorTest {

    // ──────────── CPF válidos ────────────
    @Test
    public void cpfValido_semMascara() {
        assertTrue(DocumentValidator.isValidCpf("11144477735"));
    }

    @Test
    public void cpfValido_comMascara() {
        assertTrue(DocumentValidator.isValid("111.444.777-35"));
    }

    // ──────────── CPF inválidos ────────────
    @Test
    public void cpfRepetido_recusado() {
        assertFalse(DocumentValidator.isValidCpf("11111111111"));
        assertFalse(DocumentValidator.isValidCpf("00000000000"));
    }

    @Test
    public void cpfDigitoVerificadorErrado() {
        assertFalse(DocumentValidator.isValidCpf("11144477736")); // último dígito errado
    }

    @Test
    public void cpfTamanhoErrado() {
        assertFalse(DocumentValidator.isValidCpf("123"));
        assertFalse(DocumentValidator.isValidCpf("123456789012")); // 12 dígitos
    }

    @Test
    public void cpfNullOuVazio() {
        assertFalse(DocumentValidator.isValid(null));
        assertFalse(DocumentValidator.isValid(""));
        assertFalse(DocumentValidator.isValidCpf(null));
    }

    // ──────────── CNPJ válidos ────────────
    @Test
    public void cnpjValido_semMascara() {
        assertTrue(DocumentValidator.isValidCnpj("11222333000181"));
    }

    @Test
    public void cnpjValido_comMascara() {
        assertTrue(DocumentValidator.isValid("11.222.333/0001-81"));
    }

    // ──────────── CNPJ inválidos ────────────
    @Test
    public void cnpjRepetido_recusado() {
        assertFalse(DocumentValidator.isValidCnpj("11111111111111"));
    }

    @Test
    public void cnpjDigitoVerificadorErrado() {
        assertFalse(DocumentValidator.isValidCnpj("11222333000182"));
    }

    @Test
    public void cnpjTamanhoErrado() {
        assertFalse(DocumentValidator.isValidCnpj("11222333"));
    }

    // ──────────── Roteamento isValid ────────────
    @Test
    public void documentoComTamanhoInvalido_recusado() {
        // 12 dígitos não é CPF (11) nem CNPJ (14)
        assertFalse(DocumentValidator.isValid("123456789012"));
    }
}
