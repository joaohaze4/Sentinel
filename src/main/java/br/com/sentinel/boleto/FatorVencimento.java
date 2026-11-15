package br.com.sentinel.boleto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Converte o "fator de vencimento" (4 dígitos do código de barras) em data, e vice-versa.
 *
 * <p>O fator é a quantidade de dias desde uma data-base. Como só tem 4 dígitos, ele chegou
 * no limite (9999) em 21/02/2025. A FEBRABAN então recomeçou a contagem:
 * o fator 1000 passou a valer 22/02/2025.</p>
 *
 * <p>Por isso o mesmo fator pode significar duas datas (ex.: 1656 = 2002 ou 2026).
 * O Sentinel escolhe a que fica mais perto de hoje, que é a regra usada na prática.</p>
 */
public final class FatorVencimento {

    /** Data-base original: fator 0 = 07/10/1997. */
    static final LocalDate BASE_ANTIGA = LocalDate.of(1997, 10, 7);

    /** Data-base do novo ciclo: fator 1000 = 22/02/2025. */
    static final LocalDate BASE_NOVA = LocalDate.of(2025, 2, 22);

    private FatorVencimento() {
    }

    /**
     * @param fator      os 4 dígitos do código de barras
     * @param referencia normalmente a data de hoje
     * @return a data de vencimento, ou {@code null} quando o boleto não tem vencimento (fator 0000)
     */
    public static LocalDate paraData(int fator, LocalDate referencia) {
        if (fator == 0) {
            return null;
        }

        LocalDate cicloAntigo = BASE_ANTIGA.plusDays(fator);

        if (fator < 1000) {
            return cicloAntigo; // fatores abaixo de 1000 só existem no ciclo antigo (antes de 2000)
        }

        LocalDate cicloNovo = BASE_NOVA.plusDays(fator - 1000L);

        long distanciaAntigo = Math.abs(ChronoUnit.DAYS.between(referencia, cicloAntigo));
        long distanciaNovo = Math.abs(ChronoUnit.DAYS.between(referencia, cicloNovo));

        return (distanciaNovo <= distanciaAntigo) ? cicloNovo : cicloAntigo;
    }

    /** Caminho inverso: data → fator. */
    public static int paraFator(LocalDate data) {
        if (!data.isBefore(BASE_NOVA)) {
            return 1000 + (int) ChronoUnit.DAYS.between(BASE_NOVA, data);
        }
        return (int) ChronoUnit.DAYS.between(BASE_ANTIGA, data);
    }
}
