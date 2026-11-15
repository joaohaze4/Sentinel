package br.com.sentinel.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorDeRequisicoesTest {

    // relógio "de mentira": começa em 10:00:00 de um dia qualquer
    private final AtomicLong agora = new AtomicLong(1_800_000_000_000L - (1_800_000_000_000L % 60_000));

    @Test
    void deixaPassarAteOLimiteDoCliente() {
        var limitador = new LimitadorDeRequisicoes(3, 100, agora::get);

        assertTrue(limitador.permitir("1.1.1.1"));
        assertTrue(limitador.permitir("1.1.1.1"));
        assertTrue(limitador.permitir("1.1.1.1"));
        assertFalse(limitador.permitir("1.1.1.1"));
    }

    @Test
    void cadaClienteTemSuaPropriaConta() {
        var limitador = new LimitadorDeRequisicoes(1, 100, agora::get);

        assertTrue(limitador.permitir("1.1.1.1"));
        assertFalse(limitador.permitir("1.1.1.1"));
        assertTrue(limitador.permitir("2.2.2.2"));
    }

    @Test
    void limiteGlobalSeguraMesmoComIpsDiferentes() {
        // atacante fingindo ser vários IPs
        var limitador = new LimitadorDeRequisicoes(30, 5, agora::get);

        for (int i = 0; i < 5; i++) {
            assertTrue(limitador.permitir("10.0.0." + i));
        }
        assertFalse(limitador.permitir("10.0.0.99"));
    }

    @Test
    void zeraNoMinutoSeguinte() {
        var limitador = new LimitadorDeRequisicoes(1, 100, agora::get);

        assertTrue(limitador.permitir("1.1.1.1"));
        assertFalse(limitador.permitir("1.1.1.1"));

        agora.addAndGet(60_000); // passa 1 minuto

        assertTrue(limitador.permitir("1.1.1.1"));
    }

    @Test
    void calculaSegundosParaLiberar() {
        var limitador = new LimitadorDeRequisicoes(1, 1, agora::get);

        assertEquals(60, limitador.segundosParaLiberar());
        agora.addAndGet(45_000);
        assertEquals(15, limitador.segundosParaLiberar());
    }
}
