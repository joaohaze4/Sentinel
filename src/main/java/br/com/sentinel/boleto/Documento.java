package br.com.sentinel.boleto;

import java.math.BigDecimal;

/**
 * Algo que pode ser pago com código de barras: um {@link Boleto} bancário ou uma
 * {@link ContaDeConsumo} (luz, água, telefone, impostos...).
 *
 * <p>{@code sealed} ("selada"): só essas duas classes podem implementar esta interface.
 * Assim o compilador sabe que não existe um terceiro tipo escondido.</p>
 */
public sealed interface Documento permits Boleto, ContaDeConsumo {

    Origem origem();

    String codigoDeBarras();

    String linhaDigitavel();

    BigDecimal valor();

    /** {@code true} quando o código não define valor. */
    default boolean semValor() {
        return valor().signum() == 0;
    }
}
