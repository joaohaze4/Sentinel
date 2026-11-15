package br.com.sentinel.cli;

import br.com.sentinel.boleto.EntradaInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentosTest {

    @Test
    void leCodigoEOpcoes() {
        Argumentos a = Argumentos.ler(new String[]{
                "34191.79001", "10435.100473", "--valor", "1.234,56", "--banco", "41",
                "--vencimento", "10/12/2026", "--hoje", "2026-11-15"});

        assertEquals("34191.7900110435.100473", a.codigo()); // pedaços juntados
        assertEquals(new BigDecimal("1234.56"), a.esperado().valor());
        assertEquals("041", a.esperado().codigoBanco());
        assertEquals(LocalDate.of(2026, 12, 10), a.esperado().vencimento());
        assertEquals(LocalDate.of(2026, 11, 15), a.hoje());
    }

    @Test
    void aceitaValorComPonto() {
        assertEquals(new BigDecimal("250.00"), Argumentos.lerValor("250.00"));
        assertEquals(new BigDecimal("250.00"), Argumentos.lerValor("R$ 250,00"));
    }

    @Test
    void reconheceAjuda() {
        assertTrue(Argumentos.ler(new String[]{"--ajuda"}).ajuda());
    }

    @Test
    void recusaOpcaoDesconhecida() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.ler(new String[]{"--xyz"}));
    }

    @Test
    void recusaValorQueFaltou() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.ler(new String[]{"--valor"}));
    }

    @Test
    void recusaDataInvalida() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerData("31/02/2026x"));
    }
}
