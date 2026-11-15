package br.com.sentinel.web;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.Resultado;
import br.com.sentinel.boleto.Bancos;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.relatorio.Formatador;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforma o {@link Resultado} da análise no JSON que o front-end recebe.
 *
 * <p>Exemplo (resumido):</p>
 * <pre>
 * {
 *   "classificacao": "ALTO_RISCO",
 *   "classificacaoTexto": "ALTO RISCO",
 *   "recomendacao": "NÃO pague antes de confirmar...",
 *   "pontuacao": 60,
 *   "boleto": { "banco": "237", "bancoNome": "Bradesco", "valor": "250.00", ... },
 *   "alertas": [ { "nivel": "ALTO", "titulo": "...", "detalhe": "..." } ],
 *   "aviso": "..."
 * }
 * </pre>
 */
final class RespostaJson {

    static final String AVISO = "O Sentinel confere só os números do boleto. Ele não confirma quem "
            + "recebe o dinheiro: antes de pagar, veja o nome do beneficiário no app do seu banco.";

    private RespostaJson() {
    }

    static String de(Resultado resultado, LocalDate hoje) {
        Boleto b = resultado.boleto();

        List<String> alertas = new ArrayList<>();
        for (Alerta a : resultado.alertas()) {
            alertas.add("{"
                    + "\"nivel\":" + Json.texto(a.nivel().name()) + ","
                    + "\"titulo\":" + Json.texto(a.titulo()) + ","
                    + "\"detalhe\":" + Json.texto(a.detalhe())
                    + "}");
        }

        String boleto = "{"
                + "\"linhaDigitavel\":" + Json.texto(b.linhaDigitavel()) + ","
                + "\"linhaFormatada\":" + Json.texto(Formatador.linhaDigitavel(b.linhaDigitavel())) + ","
                + "\"codigoDeBarras\":" + Json.texto(b.codigoDeBarras()) + ","
                + "\"banco\":" + Json.texto(b.codigoBanco()) + ","
                + "\"bancoNome\":" + Json.texto(Bancos.nome(b.codigoBanco()).orElse(null)) + ","
                + "\"valor\":" + Json.texto(b.semValor() ? null : b.valor().toPlainString()) + ","
                + "\"valorFormatado\":" + Json.texto(b.semValor() ? null : Formatador.moeda(b.valor())) + ","
                + "\"vencimento\":" + Json.texto(b.semVencimento() ? null : b.vencimento().toString()) + ","
                + "\"vencimentoFormatado\":"
                + Json.texto(b.semVencimento() ? null : Formatador.data(b.vencimento())) + ","
                + "\"diasParaVencer\":"
                + (b.semVencimento() ? "null" : ChronoUnit.DAYS.between(hoje, b.vencimento()))
                + "}";

        return "{"
                + "\"classificacao\":" + Json.texto(resultado.classificacao().name()) + ","
                + "\"classificacaoTexto\":" + Json.texto(resultado.classificacao().name().replace('_', ' ')) + ","
                + "\"recomendacao\":" + Json.texto(resultado.classificacao().recomendacao()) + ","
                + "\"pontuacao\":" + resultado.pontuacao() + ","
                + "\"boleto\":" + boleto + ","
                + "\"alertas\":[" + String.join(",", alertas) + "],"
                + "\"aviso\":" + Json.texto(AVISO)
                + "}";
    }
}
