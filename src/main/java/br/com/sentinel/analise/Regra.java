package br.com.sentinel.analise;

import br.com.sentinel.boleto.Documento;

import java.time.LocalDate;
import java.util.List;

/**
 * Uma verificação independente. Cada regra olha UMA coisa e devolve os alertas que achou
 * (ou uma lista vazia).
 *
 * <p>O {@code <T>} diz para qual tipo de documento a regra serve: {@code Regra<Boleto>},
 * {@code Regra<ContaDeConsumo>} ou {@code Regra<Documento>} (serve para os dois).</p>
 *
 * <p>Para criar uma regra nova, basta criar uma classe que implementa esta interface
 * e adicioná-la na lista certa do {@link AnalisadorDeRisco}. Nada mais precisa mudar.</p>
 */
public interface Regra<T extends Documento> {

    List<Alerta> avaliar(T documento, DadosEsperados esperado, LocalDate hoje);
}
