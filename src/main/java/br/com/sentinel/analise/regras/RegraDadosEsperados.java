package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Bancos;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.relatorio.Formatador;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Compara o boleto com o que o usuário esperava pagar (valor, banco e vencimento).
 *
 * <p>Esta é a regra que pega o golpe do "boleto adulterado bem feito": o código é válido,
 * todos os dígitos batem, mas o dinheiro vai para outro banco/conta.</p>
 */
public class RegraDadosEsperados implements Regra {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();

        // compareTo em vez de equals: para BigDecimal, 250.0 e 250.00 são "diferentes" no equals
        if (esperado.valor() != null && !boleto.semValor()
                && esperado.valor().compareTo(boleto.valor()) != 0) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Valor diferente do esperado",
                    "Você esperava pagar " + Formatador.moeda(esperado.valor())
                            + ", mas o boleto cobra " + Formatador.moeda(boleto.valor()) + "."));
        }

        if (esperado.codigoBanco() != null && !esperado.codigoBanco().equals(boleto.codigoBanco())) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Banco diferente do esperado",
                    "O pagamento vai para o banco " + Bancos.descricao(boleto.codigoBanco())
                            + ", mas você esperava " + Bancos.descricao(esperado.codigoBanco())
                            + ". É o golpe mais comum: trocar o código de barras para desviar o dinheiro."));
        }

        if (esperado.vencimento() != null && !esperado.vencimento().equals(boleto.vencimento())) {
            String noBoleto = boleto.semVencimento() ? "nenhum" : Formatador.data(boleto.vencimento());
            alertas.add(new Alerta(
                    Nivel.MEDIO,
                    "Vencimento diferente do esperado",
                    "Você esperava " + Formatador.data(esperado.vencimento())
                            + ", mas o boleto diz " + noBoleto + "."));
        }

        return alertas;
    }
}
