package br.com.sentinel.boleto;

/** De onde veio o número que o usuário informou. */
public enum Origem {
    /** A sequência impressa em cima do código de barras (47 dígitos no boleto, 48 na conta de consumo). */
    LINHA_DIGITAVEL,

    /** 44 dígitos, o que o leitor de código de barras captura. */
    CODIGO_DE_BARRAS
}
