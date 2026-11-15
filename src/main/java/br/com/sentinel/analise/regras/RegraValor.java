package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Documento;

import java.time.LocalDate;
import java.util.List;

/**
 * Sem valor no código: o valor é digitado na hora, então é fácil cobrar a mais.
 * Serve para os dois tipos ({@code Regra<Documento>}): boleto e conta de consumo.
 */
public class RegraValor implements Regra<Documento> {

    @Override
    public List<Alerta> avaliar(Documento documento, DadosEsperados esperado, LocalDate hoje) {
        if (!documento.semValor()) {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.BAIXO,
                "Sem valor no código",
                "O valor vai ser digitado na hora do pagamento. Confira com a fatura ou o contrato."));
    }
}
