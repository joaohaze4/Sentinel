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

    /** 48 dígitos → "83650000001-0 87520048000-7 00000001234-4 56789012345-6" */
    public static String linhaDigitavelConta(String l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 48; i += 12) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(l, i, i + 11).append('-').append(l.charAt(i + 11));
        }
        return sb.toString();
    }

    /** "12345678" → "12.345.678" (raiz do CNPJ: os 8 primeiros dígitos) */
    public static String cnpjRaiz(String r) {
        return r.substring(0, 2) + "." + r.substring(2, 5) + "." + r.substring(5, 8);
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
