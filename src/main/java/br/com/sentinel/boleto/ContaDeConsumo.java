package br.com.sentinel.boleto;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Conta de consumo ou tributo (luz, água, telefone, IPTU, multas...), no padrão de
 * ARRECADAÇÃO da FEBRABAN. Sempre começa com 8.
 *
 * <p>Estrutura do código de barras (44 dígitos):</p>
 * <pre>
 * posição  1     produto (sempre 8 = arrecadação)
 *          2     segmento (tipo de conta: 3 = energia, 2 = água...)
 *          3     identificação do valor: 6/8 = valor em reais, 7/9 = valor de referência
 *                (6/7 usam módulo 10; 8/9 usam módulo 11)
 *          4     dígito verificador geral
 *          5-15  valor (11 dígitos, em centavos)
 *          16-19 código da empresa/órgão  (no segmento 6: posições 16-23 = raiz do CNPJ)
 *          resto campo livre (cada empresa usa do seu jeito)
 * </pre>
 *
 * <p>Diferente do boleto, aqui NÃO existe banco nem vencimento padronizados no código.</p>
 */
public record ContaDeConsumo(
        Origem origem,
        String codigoDeBarras,
        String linhaDigitavel,
        char codigoSegmento,
        char identificacaoValor,
        int digitoGeral,
        BigDecimal valor,
        String empresa,
        String campoLivre) implements Documento {

    public Optional<Segmento> segmento() {
        return Segmento.de(codigoSegmento);
    }

    /** 6 ou 8: o valor está em reais. 7 ou 9: é uma quantidade de moeda/índice de referência. */
    public boolean valorEmReais() {
        return identificacaoValor == '6' || identificacaoValor == '8';
    }

    /** Só 6, 7, 8 e 9 existem; com outro valor nem dá para saber qual módulo usar. */
    public boolean identificacaoValida() {
        return identificacaoValor >= '6' && identificacaoValor <= '9';
    }

    /** No segmento 6 a empresa é identificada pela raiz do CNPJ (8 dígitos). */
    public boolean empresaPorCnpj() {
        return codigoSegmento == '6';
    }
}
