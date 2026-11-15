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

        return new Resultado(documento, List.copyOf(alertas), pontuacao, Classificacao.de(alertas));
    }
}
