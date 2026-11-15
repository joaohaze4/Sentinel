package br.com.sentinel.analise;

import br.com.sentinel.boleto.Documento;

import java.util.List;

/**
 * Tudo que a análise descobriu.
 *
 * @param pontuacao   soma dos pontos dos alertas, limitada a 100
 * @param observacoes avisos informativos que NÃO são risco (não mudam a pontuação),
 *                    ex.: "o banco que você informou não se aplica a contas de consumo"
 */
public record Resultado(Documento documento, List<Alerta> alertas, int pontuacao, Classificacao classificacao,
                        List<String> observacoes) {
}
