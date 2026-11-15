package br.com.sentinel.analise;

import br.com.sentinel.boleto.Boleto;

import java.util.List;

/**
 * Tudo que a análise descobriu.
 *
 * @param pontuacao soma dos pontos dos alertas, limitada a 100
 */
public record Resultado(Boleto boleto, List<Alerta> alertas, int pontuacao, Classificacao classificacao) {
}
