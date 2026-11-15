package br.com.sentinel.cli;

import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.boleto.EntradaInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Lê os argumentos da linha de comando.
 *
 * <pre>
 * sentinel CODIGO [--valor 250,00] [--banco 341] [--vencimento 10/12/2026] [--hoje 2026-11-15]
 * sentinel --web [--porta 8080]
 * </pre>
 */
public record Argumentos(String codigo, DadosEsperados esperado, LocalDate hoje, boolean ajuda,
                         boolean web, int porta) {

    /** Porta padrão: a variável de ambiente PORT (usada por hospedagens) ou 8080. */
    static int portaPadrao() {
        String env = System.getenv("PORT");
        return (env != null && env.matches("\\d{1,5}")) ? Integer.parseInt(env) : 8080;
    }

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Argumentos ler(String[] args) {
        String codigo = null;
        BigDecimal valor = null;
        String banco = null;
        LocalDate vencimento = null;
        LocalDate hoje = LocalDate.now();
        boolean ajuda = false;
        boolean web = false;
        int porta = portaPadrao();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-h", "--ajuda", "--help" -> ajuda = true;
                case "--valor" -> valor = lerValor(proximo(args, ++i, arg));
                case "--banco" -> banco = lerBanco(proximo(args, ++i, arg));
                case "--vencimento" -> vencimento = lerData(proximo(args, ++i, arg));
                case "--hoje" -> hoje = lerData(proximo(args, ++i, arg));
                case "--web" -> web = true;
                case "--porta" -> porta = lerPorta(proximo(args, ++i, arg));
                default -> {
                    if (arg.startsWith("--")) {
                        throw new EntradaInvalidaException("Opção desconhecida: " + arg + " (use --ajuda)");
                    }
                    // permite colar a linha com espaços sem aspas: junta os pedaços
                    codigo = (codigo == null) ? arg : codigo + arg;
                }
            }
        }

        return new Argumentos(codigo, new DadosEsperados(valor, banco, vencimento), hoje, ajuda, web, porta);
    }

    private static String proximo(String[] args, int i, String opcao) {
        if (i >= args.length) {
            throw new EntradaInvalidaException("Faltou o valor depois de " + opcao);
        }
        return args[i];
    }

    static int lerPorta(String texto) {
        String t = texto.trim();

        if (!t.matches("\\d{1,5}") || Integer.parseInt(t) > 65535) {
            throw new EntradaInvalidaException("Porta inválida: " + texto + " (exemplo: 8080)");
        }
        return Integer.parseInt(t);
    }

    /** Aceita "1.234,56", "1234,56" e "1234.56". */
    public static BigDecimal lerValor(String texto) {
        String t = texto.trim().replace("R$", "").replace(" ", "");

        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }

        try {
            return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("Valor inválido: " + texto + " (exemplo: 250,00)");
        }
    }

    /** Aceita "341" ou "41" (completa com zero à esquerda). */
    public static String lerBanco(String texto) {
        String t = texto.trim();

        if (!t.matches("\\d{1,3}")) {
            throw new EntradaInvalidaException("Código de banco inválido: " + texto + " (exemplo: 341)");
        }
        return String.format("%03d", Integer.parseInt(t));
    }

    /** Aceita "10/12/2026" ou "2026-12-10". */
    public static LocalDate lerData(String texto) {
        String t = texto.trim();

        try {
            return t.contains("/") ? LocalDate.parse(t, DATA_BR) : LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new EntradaInvalidaException("Data inválida: " + texto + " (exemplo: 10/12/2026)");
        }
    }
}
