package br.com.sentinel.boleto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.com.sentinel.Exemplos.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorDeContaDeConsumoTest {

    @Test
    void leLinhaDigitavelDaContaDeLuz() {
        ContaDeConsumo c = LeitorDeContaDeConsumo.ler(LUZ_LINHA);

        assertEquals(Origem.LINHA_DIGITAVEL, c.origem());
        assertEquals(LUZ_BARRAS, c.codigoDeBarras());
        assertEquals(Segmento.ENERGIA_E_GAS, c.segmento().orElseThrow());
        assertEquals('6', c.identificacaoValor());
        assertTrue(c.valorEmReais());
        assertEquals(new BigDecimal("187.52"), c.valor());
        assertEquals("0048", c.empresa());
        assertFalse(c.empresaPorCnpj());
    }

    @Test
    void aceitaOsTracosDaLinhaFormatada() {
        ContaDeConsumo c = LeitorDeContaDeConsumo.ler("83650000001-0 87520048000-7 00000001234-4 56789012345-6");
        assertEquals(LUZ_BARRAS, c.codigoDeBarras());
    }

    @Test
    void leCodigoDeBarrasEGeraALinha() {
        ContaDeConsumo c = LeitorDeContaDeConsumo.ler(LUZ_BARRAS);

        assertEquals(Origem.CODIGO_DE_BARRAS, c.origem());
        assertEquals(LUZ_LINHA, c.linhaDigitavel());
    }

    @Test
    void idaEVoltaComModulo11() {
        String barras = LeitorDeContaDeConsumo.linhaParaCodigoDeBarras(AGUA_LINHA);
        assertEquals(AGUA_LINHA, LeitorDeContaDeConsumo.codigoDeBarrasParaLinha(barras));
    }

    @Test
    void segmento6UsaRaizDoCnpj() {
        ContaDeConsumo c = LeitorDeContaDeConsumo.ler(CNPJ_LINHA);

        assertTrue(c.empresaPorCnpj());
        assertEquals("12345678", c.empresa());
        assertEquals(Segmento.EMPRESAS_CNPJ, c.segmento().orElseThrow());
    }

    @Test
    void valorDeReferenciaNaoEmReais() {
        assertFalse(LeitorDeContaDeConsumo.ler(REFERENCIA_LINHA).valorEmReais());
    }

    @Test
    void recusaTamanhoErrado() {
        var erro = assertThrows(EntradaInvalidaException.class, () -> LeitorDeContaDeConsumo.ler("8" + "1".repeat(46)));
        assertTrue(erro.getMessage().contains("48"));
    }

    @Test
    void leitorDeDocumentoEscolheOTipoCerto() {
        assertInstanceOf(ContaDeConsumo.class, LeitorDeDocumento.ler(LUZ_LINHA, HOJE));
        assertInstanceOf(Boleto.class, LeitorDeDocumento.ler(VALIDO_LINHA, HOJE));
    }
}
