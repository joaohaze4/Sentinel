package br.com.sentinel.boleto;

import java.time.LocalDate;

/**
 * Porta de entrada única: descobre se o texto é um boleto bancário ou uma conta de
 * consumo e chama o leitor certo. Contas de consumo sempre começam com 8.
 */
public final class LeitorDeDocumento {

    private LeitorDeDocumento() {
    }

    public static Documento ler(String entrada, LocalDate hoje) {
        String digitos = LeitorDeBoleto.limpar(entrada);

        if (digitos.startsWith("8")) {
            return LeitorDeContaDeConsumo.ler(digitos);
        }
        return LeitorDeBoleto.ler(digitos, hoje);
    }
}
