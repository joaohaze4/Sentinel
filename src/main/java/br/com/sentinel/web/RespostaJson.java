package br.com.sentinel.web;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.Resultado;
import br.com.sentinel.boleto.Bancos;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.ContaDeConsumo;
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
 *   "tipo": "BOLETO",                 (ou "CONTA_DE_CONSUMO")
 *   "classificacao": "ALTO_RISCO",
 *   "classificacaoTexto": "ALTO RISCO",
 *   "recomendacao": "NÃO pague antes de confirmar...",
 *   "pontuacao": 60,
 *   "documento": { ...campos de acordo com o tipo... },
 *   "alertas": [ { "nivel": "ALTO", "titulo": "...", "detalhe": "..." } ],
 *   "aviso": "..."
 * }
 * </pre>
 */
final class RespostaJson {

    static final String AVISO = "O Sentinel confere só os números do código. Ele não confirma quem "
            + "recebe o dinheiro: antes de pagar, veja o nome do beneficiário no app do seu banco.";

    private RespostaJson() {
    }

    static String de(Resultado resultado, LocalDate hoje) {
        String tipo;
        String documento;

        if (resultado.documento() instanceof Boleto b) {
            tipo = "BOLETO";
            documento = boleto(b, hoje);
        } else if (resultado.documento() instanceof ContaDeConsumo c) {
            tipo = "CONTA_DE_CONSUMO";
            documento = conta(c);
        } else {
            throw new IllegalStateException("Tipo de documento desconhecido");
        }

        List<String> alertas = new ArrayList<>();
        for (Alerta a : resultado.alertas()) {
            alertas.add("{"
                    + "\"nivel\":" + Json.texto(a.nivel().name()) + ","
                    + "\"titulo\":" + Json.texto(a.titulo()) + ","
                    + "\"detalhe\":" + Json.texto(a.detalhe())
                    + "}");
        }

        return "{"
                + "\"tipo\":" + Json.texto(tipo) + ","
                + "\"classificacao\":" + Json.texto(resultado.classificacao().name()) + ","
                + "\"classificacaoTexto\":" + Json.texto(resultado.classificacao().name().replace('_', ' ')) + ","
                + "\"recomendacao\":" + Json.texto(resultado.classificacao().recomendacao()) + ","
                + "\"pontuacao\":" + resultado.pontuacao() + ","
                + "\"documento\":" + documento + ","
                + "\"alertas\":[" + String.join(",", alertas) + "],"
                + "\"aviso\":" + Json.texto(AVISO)
                + "}";
    }

    private static String boleto(Boleto b, LocalDate hoje) {
        return "{"
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
    }

    private static String conta(ContaDeConsumo c) {
        String valorFormatado;
        if (c.semValor()) {
            valorFormatado = null;
        } else if (c.valorEmReais()) {
            valorFormatado = Formatador.moeda(c.valor());
        } else {
            valorFormatado = c.valor().toPlainString() + " (moeda de referência)";
        }

        return "{"
                + "\"linhaDigitavel\":" + Json.texto(c.linhaDigitavel()) + ","
                + "\"linhaFormatada\":" + Json.texto(Formatador.linhaDigitavelConta(c.linhaDigitavel())) + ","
                + "\"codigoDeBarras\":" + Json.texto(c.codigoDeBarras()) + ","
                + "\"segmento\":" + Json.texto(c.segmento().map(Enum::name).orElse(null)) + ","
                + "\"segmentoCodigo\":" + Json.texto(String.valueOf(c.codigoSegmento())) + ","
                + "\"segmentoNome\":" + Json.texto(c.segmento().map(s -> s.descricao()).orElse(null)) + ","
                + "\"empresa\":" + Json.texto(c.empresa()) + ","
                + "\"empresaPorCnpj\":" + c.empresaPorCnpj() + ","
                + "\"empresaFormatada\":" + Json.texto(c.empresaPorCnpj()
                        ? "CNPJ (raiz) " + Formatador.cnpjRaiz(c.empresa())
                        : "código " + c.empresa()) + ","
                + "\"valor\":" + Json.texto(c.semValor() ? null : c.valor().toPlainString()) + ","
                + "\"valorFormatado\":" + Json.texto(valorFormatado) + ","
                + "\"valorEmReais\":" + c.valorEmReais()
                + "}";
    }
}
