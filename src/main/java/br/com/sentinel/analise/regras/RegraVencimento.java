package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.relatorio.Formatador;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Vencimento no passado ou muito longe no futuro. */
public class RegraVencimento implements Regra<Boleto> {

    /** Mais de 1 ano no futuro é incomum para boleto. */
    static final int LIMITE_DIAS_FUTURO = 365;

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        if (boleto.semVencimento()) {
            return List.of();
        }

        long dias = ChronoUnit.DAYS.between(hoje, boleto.vencimento());

        if (dias < 0) {
            return List.of(new Alerta(
                    Nivel.MEDIO,
                    "Boleto vencido há " + (-dias) + " dia(s)",
                    "Golpistas costumam reaproveitar boletos antigos. "
                            + "Peça a 2ª via direto no site ou app de quem cobrou."));
        }

        if (dias > LIMITE_DIAS_FUTURO) {
            return List.of(new Alerta(
                    Nivel.MEDIO,
                    "Vencimento muito distante (" + Formatador.data(boleto.vencimento()) + ")",
                    "Mais de 1 ano no futuro é incomum e pode indicar que o código foi alterado."));
        }

        return List.of();
    }
}
