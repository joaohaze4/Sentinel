package br.com.sentinel.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Deixa números e datas no formato brasileiro. */
public final class Formatador {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatador() {
    }

    /** 1234.5 → "R$ 1.234,50" */
    public static String moeda(BigDecimal valor) {
        return "R$ " + String.format(BRASIL, "%,.2f", valor);
    }

    /** 2026-12-10 → "10/12/2026" */
    public static String data(LocalDate data) {
        return data.format(DATA);
    }

    /** 47 dígitos → "34191.79001 10435.100473 91020.150008 7 16560000025000" */
    public static String linhaDigitavel(String l) {
        return l.substring(0, 5) + "." + l.substring(5, 10) + " "
                + l.substring(10, 15) + "." + l.substring(15, 21) + " "
                + l.substring(21, 26) + "." + l.substring(26, 32) + " "
                + l.charAt(32) + " "
                + l.substring(33);
    }
}
