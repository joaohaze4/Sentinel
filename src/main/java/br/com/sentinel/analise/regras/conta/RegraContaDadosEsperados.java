package br.com.sentinel.analise.regras.conta;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.ContaDeConsumo;
import br.com.sentinel.relatorio.Formatador;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Compara a conta com o que o usuário esperava: valor, tipo de conta e empresa.
 *
 * <p>Golpe comum: um "boleto da conta de luz" falso, que na verdade paga outra empresa.
 * Se a pessoa diz que é conta de luz e o código é de telefone, ou se a empresa é
 * diferente da conta do mês passado, tem algo errado.</p>
 *
 * <p>Banco e vencimento esperados são ignorados aqui: contas de consumo não trazem
 * esses dados no código.</p>
 */
public class RegraContaDadosEsperados implements Regra<ContaDeConsumo> {

    @Override
    public List<Alerta> avaliar(ContaDeConsumo conta, DadosEsperados esperado, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();

        if (esperado.valor() != null && conta.valorEmReais() && !conta.semValor()
                && esperado.valor().compareTo(conta.valor()) != 0) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Valor diferente do esperado",
                    "Você esperava pagar " + Formatador.moeda(esperado.valor())
                            + ", mas a conta cobra " + Formatador.moeda(conta.valor()) + "."));
        }

        if (esperado.segmento() != null && conta.segmento().isPresent()
                && esperado.segmento() != conta.segmento().get()) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Tipo de conta diferente do esperado",
                    "Você disse que é uma conta de " + esperado.segmento().descricao()
                            + ", mas o código é de " + conta.segmento().get().descricao()
                            + ". Golpistas mandam contas falsas que se passam por contas conhecidas."));
        }

        if (esperado.empresa() != null && !esperado.empresa().equals(conta.empresa())) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Empresa diferente da conta anterior",
                    "Na conta anterior a empresa era " + esperado.empresa() + ", mas nesta é "
                            + conta.empresa() + ". O dinheiro pode estar indo para outra empresa."));
        }

        return alertas;
    }
}
