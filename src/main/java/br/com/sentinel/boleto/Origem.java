package br.com.sentinel.boleto;

/** De onde veio o número que o usuário informou. */
public enum Origem {
    /** 47 dígitos, a sequência impressa em cima do código de barras. */
    LINHA_DIGITAVEL,

    /** 44 dígitos, o que o leitor de código de barras captura. */
    CODIGO_DE_BARRAS
}
