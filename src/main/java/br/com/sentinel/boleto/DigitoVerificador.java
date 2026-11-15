package br.com.sentinel.boleto;

/**
 * Cálculo dos dígitos verificadores do boleto bancário (padrão FEBRABAN).
 *
 * <p>Dígito verificador (DV) é um número calculado a partir dos outros dígitos.
 * Se alguém digitar um número errado ou alterar o código, a conta deixa de bater.
 * É a primeira linha de defesa contra erro de digitação e adulteração "amadora".</p>
 */
public final class DigitoVerificador {

    private DigitoVerificador() {
        // classe utilitária: só tem métodos estáticos, não precisa ser instanciada
    }

    /**
     * Módulo 10: usado nos campos 1, 2 e 3 da linha digitável.
     *
     * <p>Passo a passo:</p>
     * <ol>
     *   <li>Da direita para a esquerda, multiplica cada dígito por 2, 1, 2, 1...</li>
     *   <li>Se o produto passar de 9, soma os algarismos dele (16 vira 1 + 6 = 7).</li>
     *   <li>Soma tudo.</li>
     *   <li>O DV é o que falta para chegar na próxima dezena (soma 23 → DV 7; soma 30 → DV 0).</li>
     * </ol>
     */
    public static int modulo10(String numeros) {
        int soma = 0;
        int peso = 2;

        for (int i = numeros.length() - 1; i >= 0; i--) {
            int produto = digito(numeros, i) * peso;

            if (produto > 9) {
                produto = produto - 9; // dá o mesmo que somar os dois algarismos (16 - 9 = 7)
            }

            soma += produto;
            peso = (peso == 2) ? 1 : 2;
        }

        int resto = soma % 10;
        return (resto == 0) ? 0 : 10 - resto;
    }

    /**
     * Módulo 11: usado no dígito verificador geral do código de barras.
     *
     * <p>Passo a passo:</p>
     * <ol>
     *   <li>Da direita para a esquerda, multiplica cada dígito por 2, 3, 4 ... 9 e recomeça no 2.</li>
     *   <li>Soma tudo e pega o resto da divisão por 11.</li>
     *   <li>DV = 11 - resto. Se der 10 ou 11, o boleto bancário usa 1.</li>
     * </ol>
     */
    public static int modulo11(String numeros) {
        int soma = 0;
        int peso = 2;

        for (int i = numeros.length() - 1; i >= 0; i--) {
            soma += digito(numeros, i) * peso;
            peso = (peso == 9) ? 2 : peso + 1;
        }

        int dv = 11 - (soma % 11);

        if (dv == 10 || dv == 11) {
            return 1;
        }
        return dv;
    }

    private static int digito(String numeros, int posicao) {
        return numeros.charAt(posicao) - '0'; // '7' - '0' = 7 (truque com a tabela de caracteres)
    }
}
