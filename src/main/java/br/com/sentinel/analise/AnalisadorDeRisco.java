package br.com.sentinel.analise;

import br.com.sentinel.analise.regras.RegraBancoConhecido;
import br.com.sentinel.analise.regras.RegraDadosEsperados;
import br.com.sentinel.analise.regras.RegraDigitoGeral;
import br.com.sentinel.analise.regras.RegraDigitosDosCampos;
import br.com.sentinel.analise.regras.RegraMoeda;
import br.com.sentinel.analise.regras.RegraValor;
import br.com.sentinel.analise.regras.RegraVencimento;
import br.com.sentinel.boleto.Boleto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Roda todas as regras e junta o resultado.
 *
 * <p>Padrão de projeto usado: cada regra é uma peça separada (padrão "Strategy").
 * O analisador não sabe os detalhes de nenhuma regra, só manda cada uma avaliar.</p>
 */
public class AnalisadorDeRisco {

    private final List<Regra> regras;

    /** Analisador com todas as regras padrão do Sentinel. */
    public AnalisadorDeRisco() {
        this(List.of(
                new RegraDigitosDosCampos(),
                new RegraDigitoGeral(),
                new RegraMoeda(),
                new RegraBancoConhecido(),
                new RegraValor(),
                new RegraVencimento(),
                new RegraDadosEsperados()));
    }

    /** Permite escolher as regras (útil em testes ou para ligar/desligar verificações). */
    public AnalisadorDeRisco(List<Regra> regras) {
        this.regras = List.copyOf(regras);
    }

    public Resultado analisar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();

        for (Regra regra : regras) {
            alertas.addAll(regra.avaliar(boleto, esperado, hoje));
        }

        // mais graves primeiro
        alertas.sort(Comparator.comparing(Alerta::nivel, Comparator.reverseOrder()));

        int pontuacao = Math.min(100, alertas.stream().mapToInt(a -> a.nivel().pontos()).sum());

        return new Resultado(boleto, List.copyOf(alertas), pontuacao, Classificacao.de(alertas));
    }
}
