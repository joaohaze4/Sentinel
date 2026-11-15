package br.com.sentinel.cli;

import br.com.sentinel.boleto.EntradaInvalidaException;
import br.com.sentinel.boleto.Segmento;
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
    void leModoWebEPorta() {
        Argumentos a = Argumentos.ler(new String[]{"--web", "--porta", "9090"});

        assertTrue(a.web());
        assertEquals(9090, a.porta());
    }

    @Test
    void recusaPortaInvalida() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.ler(new String[]{"--porta", "99999"}));
    }

    @Test
    void leTipoDeContaPorApelidoOuNome() {
        assertEquals(Segmento.ENERGIA_E_GAS, Argumentos.lerTipo("luz"));
        assertEquals(Segmento.SANEAMENTO, Argumentos.lerTipo("Água"));
        assertEquals(Segmento.TELECOMUNICACOES, Argumentos.lerTipo("TELECOMUNICACOES"));
    }

    @Test
    void recusaTipoForaDaLista() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerTipo("<script>alert(1)</script>"));
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerTipo("USO_DO_BANCO_x"));
    }

    @Test
    void leEmpresa() {
        assertEquals("0048", Argumentos.lerEmpresa("0048"));
        assertEquals("12345678", Argumentos.lerEmpresa("12.345.678"));
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerEmpresa("12"));
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerEmpresa("abcd"));
    }

    @Test
    void limitarTiraCaracteresPerigosos() {
        assertEquals("?script?alert?1??/script?", Argumentos.limitar("<script>alert(1)</script>"));
        assertEquals("Água 12,50", Argumentos.limitar("Água 12,50"));
    }

    @Test
    void mensagemDeErroNaoDevolveTextoGigante() {
        var erro = assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerTipo("x".repeat(500)));
        assertTrue(erro.getMessage().length() < 200);
    }

    @Test
    void leOpcoesDeConta() {
        Argumentos a = Argumentos.ler(new String[]{"8365", "--tipo", "energia", "--empresa", "0048"});

        assertEquals(Segmento.ENERGIA_E_GAS, a.esperado().segmento());
        assertEquals("0048", a.esperado().empresa());
    }

    @Test
    void recusaDataInvalida() {
        assertThrows(EntradaInvalidaException.class, () -> Argumentos.lerData("31/02/2026x"));
    }
}
