package br.com.sentinel.boleto;

import java.math.BigDecimal;

/**
 * Transforma o texto digitado numa {@link ContaDeConsumo}.
 *
 * <p>A linha digitável (48 dígitos) é o código de barras (44) dividido em 4 blocos
 * de 11 dígitos, cada um seguido do seu dígito verificador:</p>
 * <pre>
 * 83650000001-0  87520048000-7  00000001234-4  56789012345-6
 * └ bloco 1 ┘ DV └ bloco 2 ┘ DV └ bloco 3 ┘ DV └ bloco 4 ┘ DV
 * </pre>
 */
public final class LeitorDeContaDeConsumo {

    private static final int TAMANHO_LINHA_DIGITAVEL = 48;
    private static final int TAMANHO_CODIGO_DE_BARRAS = 44;

    private LeitorDeContaDeConsumo() {
    }

    public static ContaDeConsumo ler(String entrada) {
        String digitos = LeitorDeBoleto.limpar(entrada);

        if (!digitos.startsWith("8")) {
            throw new EntradaInvalidaException("Contas de consumo sempre começam com 8.");
        }

        if (digitos.length() == TAMANHO_LINHA_DIGITAVEL) {
            return montar(Origem.LINHA_DIGITAVEL, linhaParaCodigoDeBarras(digitos), digitos);
        }

        if (digitos.length() == TAMANHO_CODIGO_DE_BARRAS) {
            return montar(Origem.CODIGO_DE_BARRAS, digitos, codigoDeBarrasParaLinha(digitos));
        }

        throw new EntradaInvalidaException(
                "Essa conta de consumo tem " + digitos.length() + " dígitos. "
                        + "A linha digitável tem 48 e o código de barras tem 44.");
    }

    private static ContaDeConsumo montar(Origem origem, String barras, String linha) {
        char segmento = barras.charAt(1);
        boolean porCnpj = segmento == '6';
        int fimEmpresa = porCnpj ? 23 : 19;

        return new ContaDeConsumo(
                origem,
                barras,
                linha,
                segmento,
                barras.charAt(2),
                barras.charAt(3) - '0',
                BigDecimal.valueOf(Long.parseLong(barras.substring(4, 15)), 2),
                barras.substring(15, fimEmpresa),
                barras.substring(fimEmpresa));
    }

    /** Tira o DV do fim de cada bloco: 4 x (11 dígitos + DV) → 44 dígitos. */
    public static String linhaParaCodigoDeBarras(String linha) {
        return linha.substring(0, 11)
                + linha.substring(12, 23)
                + linha.substring(24, 35)
                + linha.substring(36, 47);
    }

    /** Caminho inverso: divide em 4 blocos e calcula o DV de cada um. */
    public static String codigoDeBarrasParaLinha(String barras) {
        char identificacao = barras.charAt(2);
        StringBuilder linha = new StringBuilder();

        for (int i = 0; i < 44; i += 11) {
            String bloco = barras.substring(i, i + 11);
            linha.append(bloco).append(DigitoVerificador.arrecadacao(identificacao, bloco));
        }
        return linha.toString();
    }
}
