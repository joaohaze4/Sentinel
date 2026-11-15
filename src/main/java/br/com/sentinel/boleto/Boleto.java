package br.com.sentinel.boleto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Os dados de um boleto bancário, já separados.
 *
 * <p>É um {@code record}: uma classe só para guardar dados, que o Java gera
 * com construtor, getters ({@code boleto.valor()}), equals e toString prontos.</p>
 *
 * <p>Estrutura do código de barras (44 dígitos):</p>
 * <pre>
 * posição  1-3   banco
 *          4     moeda (9 = real)
 *          5     dígito verificador geral
 *          6-9   fator de vencimento
 *          10-19 valor (em centavos)
 *          20-44 campo livre (cada banco usa do seu jeito: agência, conta, nosso número...)
 * </pre>
 */
public record Boleto(
        Origem origem,
        String codigoDeBarras,
        String linhaDigitavel,
        String codigoBanco,
        char moeda,
        int digitoGeral,
        int fatorVencimento,
        LocalDate vencimento,
        BigDecimal valor,
        String campoLivre) {

    /** {@code true} quando o boleto não define valor (ele é digitado na hora de pagar). */
    public boolean semValor() {
        return valor.signum() == 0;
    }

    /** {@code true} quando o boleto não tem data de vencimento (fator 0000). */
    public boolean semVencimento() {
        return vencimento == null;
    }
}
