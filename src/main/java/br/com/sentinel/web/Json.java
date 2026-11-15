package br.com.sentinel.web;

/**
 * Ajudante mínimo para escrever JSON sem precisar de biblioteca.
 *
 * <p>O cuidado principal é "escapar" o texto: aspas, barras e quebras de linha
 * dentro de uma string quebrariam o JSON (e poderiam virar brecha de segurança).</p>
 */
final class Json {

    private Json() {
    }

    /** {@code Olá "mundo"} → {@code "Olá \"mundo\""}; {@code null} → {@code null}. */
    static String texto(String valor) {
        if (valor == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder(valor.length() + 2).append('"');

        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c)); // outros caracteres de controle
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** {"erro": "mensagem"} */
    static String erro(String mensagem) {
        return "{\"erro\":" + texto(mensagem) + "}";
    }
}
