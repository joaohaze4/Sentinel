package br.com.sentinel.analise;

import br.com.sentinel.analise.regras.RegraBancoConhecido;
import br.com.sentinel.analise.regras.RegraDadosEsperados;
import br.com.sentinel.analise.regras.RegraDigitoGeral;
import br.com.sentinel.analise.regras.RegraDigitosDosCampos;
import br.com.sentinel.analise.regras.RegraMoeda;
import br.com.sentinel.analise.regras.RegraValor;
import br.com.sentinel.analise.regras.RegraVencimento;
import br.com.sentinel.analise.regras.conta.RegraContaDadosEsperados;
import br.com.sentinel.analise.regras.conta.RegraContaDigitoGeral;
import br.com.sentinel.analise.regras.conta.RegraContaDigitosDosBlocos;
import br.com.sentinel.analise.regras.conta.RegraContaIdentificacaoValor;
import br.com.sentinel.analise.regras.conta.RegraContaSegmento;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.ContaDeConsumo;
import br.com.sentinel.boleto.Documento;

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

    /*
     * "Regra<? super Boleto>" = uma regra que sabe avaliar Boleto OU algo mais geral (Documento).
     * É isso que deixa a RegraValor (Regra<Documento>) entrar nas duas listas.
     */
    private final List<Regra<? super Boleto>> regrasBoleto;
    private final List<Regra<? super ContaDeConsumo>> regrasConta;

    /** Analisador com todas as regras padrão do Sentinel. */
    public AnalisadorDeRisco() {
        this(
                List.of(
                        new RegraDigitosDosCampos(),
                        new RegraDigitoGeral(),
                        new RegraMoeda(),
                        new RegraBancoConhecido(),
                        new RegraValor(),
                        new RegraVencimento(),
                        new RegraDadosEsperados()),
                List.of(
                        new RegraContaDigitosDosBlocos(),
                        new RegraContaDigitoGeral(),
                        new RegraContaIdentificacaoValor(),
                        new RegraContaSegmento(),
                        new RegraValor(),
                        new RegraContaDadosEsperados()));
    }

    /** Permite escolher as regras (útil em testes ou para ligar/desligar verificações). */
    public AnalisadorDeRisco(List<Regra<? super Boleto>> regrasBoleto,
                             List<Regra<? super ContaDeConsumo>> regrasConta) {
        this.regrasBoleto = List.copyOf(regrasBoleto);
        this.regrasConta = List.copyOf(regrasConta);
    }

    public Resultado analisar(Documento documento, DadosEsperados esperado, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();

        // "instanceof com variável" (Java 16+): testa o tipo e já cria a variável convertida
        if (documento instanceof Boleto boleto) {
            for (Regra<? super Boleto> regra : regrasBoleto) {
                alertas.addAll(regra.avaliar(boleto, esperado, hoje));
            }
        } else if (documento instanceof ContaDeConsumo conta) {
            for (Regra<? super ContaDeConsumo> regra : regrasConta) {
                alertas.addAll(regra.avaliar(conta, esperado, hoje));
            }
        }

        // mais graves primeiro
        alertas.sort(Comparator.comparing(Alerta::nivel, Comparator.reverseOrder()));

        int pontuacao = Math.min(100, alertas.stream().mapToInt(a -> a.nivel().pontos()).sum());

        return new Resultado(documento, List.copyOf(alertas), pontuacao, Classificacao.de(alertas),
                observacoes(documento, esperado));
    }

    /**
     * Avisa quando o usuário preencheu algo que não se aplica ao tipo de documento,
     * para ele não achar que aquilo foi conferido. Não é alerta de risco.
     */
    static List<String> observacoes(Documento documento, DadosEsperados esperado) {
        List<String> observacoes = new ArrayList<>();

        if (documento instanceof ContaDeConsumo) {
            boolean banco = esperado.codigoBanco() != null;
            boolean vencimento = esperado.vencimento() != null;

            if (banco && vencimento) {
                observacoes.add("O banco e o vencimento que você informou foram ignorados: "
                        + "contas de consumo não trazem esses dados no código.");
            } else if (banco) {
                observacoes.add("O banco que você informou foi ignorado: "
                        + "contas de consumo não trazem banco no código.");
            } else if (vencimento) {
                observacoes.add("O vencimento que você informou foi ignorado: "
                        + "contas de consumo não trazem vencimento no código. Confira a data na própria conta.");
            }
        }

        if (documento instanceof Boleto) {
            boolean tipo = esperado.segmento() != null;
            boolean empresa = esperado.empresa() != null;

            if (tipo && empresa) {
                observacoes.add("O tipo de conta e a empresa que você informou foram ignorados: "
                        + "eles só valem para contas de consumo, e isto é um boleto bancário.");
            } else if (tipo) {
                observacoes.add("O tipo de conta que você informou foi ignorado: "
                        + "ele só vale para contas de consumo, e isto é um boleto bancário.");
            } else if (empresa) {
                observacoes.add("A empresa que você informou foi ignorada: "
                        + "ela só vale para contas de consumo, e isto é um boleto bancário.");
            }
        }

        return List.copyOf(observacoes);
    }
}
