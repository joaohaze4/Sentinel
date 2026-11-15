package br.com.sentinel.boleto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.sentinel.Exemplos.HOJE;
import static br.com.sentinel.Exemplos.VALIDO_BARRAS;
import static br.com.sentinel.Exemplos.VALIDO_LINHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorDeBoletoTest {

    @Test
    void leLinhaDigitavel() {
        Boleto b = LeitorDeBoleto.ler(VALIDO_LINHA, HOJE);

        assertEquals(Origem.LINHA_DIGITAVEL, b.origem());
        assertEquals("341", b.codigoBanco());
        assertEquals('9', b.moeda());
        assertEquals(7, b.digitoGeral());
        assertEquals(new BigDecimal("250.00"), b.valor());
        assertEquals(LocalDate.of(2026, 12, 10), b.vencimento());
        assertEquals(VALIDO_BARRAS, b.codigoDeBarras());
    }

    @Test
    void ignoraPontosEEspacos() {
        Boleto b = LeitorDeBoleto.ler("34191.79001 10435.100473 91020.150008 7 16560000025000", HOJE);
        assertEquals(VALIDO_BARRAS, b.codigoDeBarras());
    }

    @Test
    void leCodigoDeBarrasEGeraALinha() {
        Boleto b = LeitorDeBoleto.ler(VALIDO_BARRAS, HOJE);

        assertEquals(Origem.CODIGO_DE_BARRAS, b.origem());
        assertEquals(VALIDO_LINHA, b.linhaDigitavel());
    }

    @Test
    void idaEVoltaEntreLinhaECodigoDeBarras() {
        String barras = LeitorDeBoleto.linhaParaCodigoDeBarras(VALIDO_LINHA);
        assertEquals(VALIDO_LINHA, LeitorDeBoleto.codigoDeBarrasParaLinha(barras));
    }

    @Test
    void recusaTamanhoErrado() {
        var erro = assertThrows(EntradaInvalidaException.class, () -> LeitorDeBoleto.ler("123456", HOJE));
        assertTrue(erro.getMessage().contains("6 dígitos"));
    }

    @Test
    void recusaLetras() {
        assertThrows(EntradaInvalidaException.class, () -> LeitorDeBoleto.ler("3419A" + "0".repeat(42), HOJE));
    }

    @Test
    void recusaEntradaVazia() {
        assertThrows(EntradaInvalidaException.class, () -> LeitorDeBoleto.ler("   ", HOJE));
    }

    @Test
    void leitorDeBoletoRecusaContaDeConsumo() {
        // quem lê conta de consumo é o LeitorDeContaDeConsumo (via LeitorDeDocumento)
        var erro = assertThrows(EntradaInvalidaException.class,
                () -> LeitorDeBoleto.ler("8" + "1".repeat(47), HOJE));
        assertTrue(erro.getMessage().contains("conta de consumo"));
    }
}
