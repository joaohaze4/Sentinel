package br.com.sentinel.analise.regras.conta;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.ContaDeConsumo;
import br.com.sentinel.boleto.DigitoVerificador;

import java.time.LocalDate;
import java.util.List;

/** Confere o dígito verificador geral da conta (4ª posição do código de barras). */
public class RegraContaDigitoGeral implements Regra<ContaDeConsumo> {

    @Override
    public List<Alerta> avaliar(ContaDeConsumo conta, DadosEsperados esperado, LocalDate hoje) {
        if (!conta.identificacaoValida()) {
            return List.of();
        }

        String barras = conta.codigoDeBarras();
        String semDigito = barras.substring(0, 3) + barras.substring(4);
        int calculado = DigitoVerificador.arrecadacao(conta.identificacaoValor(), semDigito);

        if (calculado == conta.digitoGeral()) {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.ALTO,
                "Dígito verificador geral não confere",
                "O código informa " + conta.digitoGeral() + ", mas a conta dá " + calculado
                        + ". Algum número foi digitado errado ou alterado."));
    }
}
