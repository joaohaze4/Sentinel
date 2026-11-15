package br.com.sentinel.analise.regras.conta;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.ContaDeConsumo;

import java.time.LocalDate;
import java.util.List;

/** Confere a 3ª posição (identificação do valor): só 6, 7, 8 e 9 existem. */
public class RegraContaIdentificacaoValor implements Regra<ContaDeConsumo> {

    @Override
    public List<Alerta> avaliar(ContaDeConsumo conta, DadosEsperados esperado, LocalDate hoje) {
        if (!conta.identificacaoValida()) {
            return List.of(new Alerta(
                    Nivel.ALTO,
                    "Código de valor inválido",
                    "A 3ª posição de uma conta de consumo deve ser 6, 7, 8 ou 9, mas aqui está "
                            + conta.identificacaoValor() + ". Esse código não segue o padrão dos bancos."));
        }

        if (!conta.valorEmReais()) {
            return List.of(new Alerta(
                    Nivel.BAIXO,
                    "Valor em moeda de referência",
                    "O valor no código não está em reais: é uma quantidade de índice ou moeda de "
                            + "referência. Confira na conta quanto isso dá em reais."));
        }

        return List.of();
    }
}
