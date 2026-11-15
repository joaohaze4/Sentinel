package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Boleto;

import java.time.LocalDate;
import java.util.List;

/** Boleto sem valor no código: o valor é digitado na hora, então é fácil cobrar a mais. */
public class RegraValor implements Regra {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        if (!boleto.semValor()) {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.BAIXO,
                "Boleto sem valor no código",
                "O valor vai ser digitado na hora do pagamento. Confira com a fatura ou o contrato."));
    }
}
