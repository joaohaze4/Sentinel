package br.com.sentinel.boleto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FatorVencimentoTest {

    @Test
    void fatorZeroSignificaSemVencimento() {
        assertNull(FatorVencimento.paraData(0, LocalDate.of(2026, 11, 15)));
    }

    @Test
    void fator1000NoCicloAntigoEra2000() {
        assertEquals(LocalDate.of(2000, 7, 3), FatorVencimento.paraData(1000, LocalDate.of(2000, 6, 1)));
    }

    @Test
    void fator1000NoCicloNovoE22DeFevereiroDe2025() {
        assertEquals(LocalDate.of(2025, 2, 22), FatorVencimento.paraData(1000, LocalDate.of(2025, 3, 1)));
    }

    @Test
    void escolheODataMaisPertoDeHoje() {
        // 1656 pode ser 2002 ou 2026; em 2026 tem que ser 2026
        assertEquals(LocalDate.of(2026, 12, 10), FatorVencimento.paraData(1656, LocalDate.of(2026, 11, 15)));
    }

    @Test
    void ultimoDiaDoCicloAntigo() {
        assertEquals(LocalDate.of(2025, 2, 21), FatorVencimento.paraData(9999, LocalDate.of(2025, 2, 20)));
    }

    @Test
    void converteDataEmFator() {
        assertEquals(1631, FatorVencimento.paraFator(LocalDate.of(2026, 11, 15)));
        assertEquals(1000, FatorVencimento.paraFator(LocalDate.of(2025, 2, 22)));
        assertEquals(9999, FatorVencimento.paraFator(LocalDate.of(2025, 2, 21)));
    }
}
