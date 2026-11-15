package br.com.sentinel;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * A data de "hoje" no horário de Brasília.
 *
 * <p>Por que isso importa: servidores na nuvem (como o Render) costumam rodar no horário
 * UTC, 3 horas à frente do Brasil. Com {@code LocalDate.now()}, das 21h à meia-noite o
 * servidor já acharia que é "amanhã", e um boleto que vence hoje apareceria como vencido.</p>
 */
public final class HorarioBrasil {

    /** Fuso oficial de Brasília (já sabe lidar com horário de verão, se ele voltar). */
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private HorarioBrasil() {
    }

    public static LocalDate hoje() {
        return hoje(Clock.systemUTC());
    }

    /** Versão testável: nos testes passamos um relógio parado num instante conhecido. */
    public static LocalDate hoje(Clock relogio) {
        return LocalDate.now(relogio.withZone(ZONA));
    }
}
