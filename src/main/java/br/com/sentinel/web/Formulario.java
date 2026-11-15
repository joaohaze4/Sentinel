package br.com.sentinel.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lê o corpo de um formulário web ("application/x-www-form-urlencoded").
 *
 * <p>É o formato que o navegador envia: {@code codigo=3419...&valor=250%2C00&banco=341}.
 * Cada par vem separado por {@code &}, e caracteres especiais vêm codificados
 * ({@code %2C} é a vírgula).</p>
 */
final class Formulario {

    private Formulario() {
    }

    static Map<String, String> ler(String corpo) {
        Map<String, String> campos = new HashMap<>();

        if (corpo == null || corpo.isBlank()) {
            return campos;
        }

        for (String par : corpo.split("&")) {
            if (par.isEmpty()) {
                continue;
            }

            int igual = par.indexOf('=');
            String nome = (igual >= 0) ? par.substring(0, igual) : par;
            String valor = (igual >= 0) ? par.substring(igual + 1) : "";

            campos.put(decodificar(nome), decodificar(valor));
        }
        return campos;
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }
}
