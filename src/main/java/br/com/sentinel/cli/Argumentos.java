package br.com.sentinel.cli;

import br.com.sentinel.HorarioBrasil;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.boleto.EntradaInvalidaException;
import br.com.sentinel.boleto.Segmento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Lê os argumentos da linha de comando.
 *
 * <pre>
 * sentinel CODIGO [--valor 250,00] [--banco 341] [--vencimento 10/12/2026] [--hoje 2026-11-15]
 * sentinel CONTA  [--valor 187,52] [--tipo energia] [--empresa 0048]
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
        Segmento tipo = null;
        String empresa = null;
        LocalDate hoje = HorarioBrasil.hoje();
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
                case "--tipo" -> tipo = lerTipo(proximo(args, ++i, arg));
                case "--empresa" -> empresa = lerEmpresa(proximo(args, ++i, arg));
                case "--hoje" -> hoje = lerData(proximo(args, ++i, arg));
                case "--web" -> web = true;
                case "--porta" -> porta = lerPorta(proximo(args, ++i, arg));
                default -> {
                    if (arg.startsWith("--")) {
                        throw new EntradaInvalidaException("Opção desconhecida: " + limitar(arg) + " (use --ajuda)");
                    }
                    // permite colar a linha com espaços sem aspas: junta os pedaços
                    codigo = (codigo == null) ? arg : codigo + arg;
                }
            }
        }

        DadosEsperados esperado = new DadosEsperados(valor, banco, vencimento, tipo, empresa);
        return new Argumentos(codigo, esperado, hoje, ajuda, web, porta);
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
            throw new EntradaInvalidaException("Porta inválida: " + limitar(texto) + " (exemplo: 8080)");
        }
        return Integer.parseInt(t);
    }

    /** Apelidos aceitos em --tipo (além do nome exato, como ENERGIA_E_GAS). */
    private static final Map<String, Segmento> APELIDOS_TIPO = Map.ofEntries(
            Map.entry("energia", Segmento.ENERGIA_E_GAS),
            Map.entry("luz", Segmento.ENERGIA_E_GAS),
            Map.entry("gas", Segmento.ENERGIA_E_GAS),
            Map.entry("agua", Segmento.SANEAMENTO),
            Map.entry("esgoto", Segmento.SANEAMENTO),
            Map.entry("telefone", Segmento.TELECOMUNICACOES),
            Map.entry("internet", Segmento.TELECOMUNICACOES),
            Map.entry("prefeitura", Segmento.PREFEITURAS),
            Map.entry("iptu", Segmento.PREFEITURAS),
            Map.entry("governo", Segmento.ORGAOS_GOVERNAMENTAIS),
            Map.entry("multa", Segmento.MULTAS_DE_TRANSITO),
            Map.entry("empresa", Segmento.EMPRESAS_CNPJ),
            Map.entry("carne", Segmento.EMPRESAS_CNPJ));

    /**
     * Tipo de conta esperado: "energia", "agua", "telefone"... ou o nome exato (ENERGIA_E_GAS).
     * Só aceita valores da lista: qualquer outra coisa é recusada (lista de permitidos).
     */
    public static Segmento lerTipo(String texto) {
        String t = texto.trim();
        // "Água" → "agua": tira acentos (separa a letra do acento e apaga o acento) e põe em minúsculas
        String minusculo = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        if (APELIDOS_TIPO.containsKey(minusculo)) {
            return APELIDOS_TIPO.get(minusculo);
        }

        for (Segmento s : Segmento.values()) {
            if (s.name().equals(t)) {
                return s;
            }
        }

        throw new EntradaInvalidaException("Tipo de conta inválido: " + limitar(texto)
                + " (use: energia, agua, telefone, prefeitura, governo, multa ou empresa)");
    }

    /** Código da empresa (4 dígitos) ou raiz do CNPJ (8 dígitos), da conta anterior. */
    public static String lerEmpresa(String texto) {
        String t = texto.trim().replace(".", "");

        if (!t.matches("\\d{4}|\\d{8}")) {
            throw new EntradaInvalidaException("Empresa inválida: " + limitar(texto)
                    + " (são 4 dígitos, ou os 8 primeiros do CNPJ)");
        }
        return t;
    }

    /**
     * Prepara o texto do usuário para aparecer numa mensagem de erro:
     * corta em 30 caracteres e troca por "?" tudo que não for letra, número ou pontuação simples.
     * Assim a mensagem nunca "devolve" HTML ou script que alguém tenha mandado
     * (defesa em profundidade contra XSS refletido).
     */
    static String limitar(String texto) {
        String curto = texto.length() > 30 ? texto.substring(0, 30) + "..." : texto;
        return curto.replaceAll("[^\\p{L}\\p{N} ,./-]", "?");
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
            throw new EntradaInvalidaException("Valor inválido: " + limitar(texto) + " (exemplo: 250,00)");
        }
    }

    /** Aceita "341" ou "41" (completa com zero à esquerda). */
    public static String lerBanco(String texto) {
        String t = texto.trim();

        if (!t.matches("\\d{1,3}")) {
            throw new EntradaInvalidaException("Código de banco inválido: " + limitar(texto) + " (exemplo: 341)");
        }
        return String.format("%03d", Integer.parseInt(t));
    }

    /** Aceita "10/12/2026" ou "2026-12-10". */
    public static LocalDate lerData(String texto) {
        String t = texto.trim();

        try {
            return t.contains("/") ? LocalDate.parse(t, DATA_BR) : LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new EntradaInvalidaException("Data inválida: " + limitar(texto) + " (exemplo: 10/12/2026)");
        }
    }
}
