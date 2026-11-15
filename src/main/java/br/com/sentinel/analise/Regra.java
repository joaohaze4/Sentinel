package br.com.sentinel.analise;

import br.com.sentinel.boleto.Boleto;

import java.time.LocalDate;
import java.util.List;

/**
 * Uma verificação independente. Cada regra olha UMA coisa e devolve os alertas que achou
 * (ou uma lista vazia).
 *
 * <p>Para criar uma regra nova, basta criar uma classe que implementa esta interface
 * e adicioná-la na lista do {@link AnalisadorDeRisco}. Nada mais precisa mudar.</p>
 */
public interface Regra {

    List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje);
}
