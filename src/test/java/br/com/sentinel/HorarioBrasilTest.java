package br.com.sentinel;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HorarioBrasilTest {

    private static Clock paradoEm(String instanteUtc) {
        return Clock.fixed(Instant.parse(instanteUtc), ZoneOffset.UTC);
    }

    @Test
    void as22hDeBrasiliaAindaEHojeMesmoComUtcJaNoDiaSeguinte() {
        // 01:30 UTC do dia 16 = 22:30 de Brasília do dia 15 (o bug que estava acontecendo no Render)
        assertEquals(LocalDate.of(2026, 11, 15), HorarioBrasil.hoje(paradoEm("2026-11-16T01:30:00Z")));
    }

    @Test
    void depoisDaMeiaNoiteDeBrasiliaViraODia() {
        // 03:00 UTC = 00:00 de Brasília
        assertEquals(LocalDate.of(2026, 11, 16), HorarioBrasil.hoje(paradoEm("2026-11-16T03:00:00Z")));
    }

    @Test
    void usaOFusoDeSaoPaulo() {
        assertEquals("America/Sao_Paulo", HorarioBrasil.ZONA.getId());
    }
}
