package br.com.sentinel.analise;

/**
 * Um sinal suspeito encontrado no boleto.
 *
 * @param nivel   gravidade
 * @param titulo  frase curta (ex.: "Dígito do campo 2 não confere")
 * @param detalhe explicação em linguagem simples e o que fazer
 */
public record Alerta(Nivel nivel, String titulo, String detalhe) {
}
