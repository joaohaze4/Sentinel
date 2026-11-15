package br.com.sentinel.boleto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transforma o texto digitado (linha digitável ou código de barras) num {@link Boleto}.
 *
 * <p>Importante: o leitor só recusa o que nem parece boleto (tamanho errado, letras).
 * Dígito verificador errado NÃO é recusado aqui, porque isso é justamente um sinal
 * de fraude que as regras de análise precisam ver e explicar ao usuário.</p>
 */
public final class LeitorDeBoleto {

    private static final int TAMANHO_LINHA_DIGITAVEL = 47;
    private static final int TAMANHO_CODIGO_DE_BARRAS = 44;

    private LeitorDeBoleto() {
    }

    public static Boleto ler(String entrada, LocalDate hoje) {
        String digitos = limpar(entrada);

        // Contas de consumo (luz, água, impostos) começam com 8 e seguem outro padrão:
        // quem cuida delas é o LeitorDeContaDeConsumo (o LeitorDeDocumento escolhe o certo).
        if (digitos.startsWith("8")) {
            throw new EntradaInvalidaException(
                    "Isso é uma conta de consumo (começa com 8), não um boleto bancário.");
        }

        if (digitos.length() == TAMANHO_LINHA_DIGITAVEL) {
            return montar(Origem.LINHA_DIGITAVEL, linhaParaCodigoDeBarras(digitos), digitos, hoje);
        }

        if (digitos.length() == TAMANHO_CODIGO_DE_BARRAS) {
            return montar(Origem.CODIGO_DE_BARRAS, digitos, codigoDeBarrasParaLinha(digitos), hoje);
        }

        throw new EntradaInvalidaException(
                "O código tem " + digitos.length() + " dígitos. "
                        + "A linha digitável tem 47 e o código de barras tem 44.");
    }

    /** Tira espaços, pontos e traços, e garante que sobrou só número. */
    static String limpar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new EntradaInvalidaException("Nenhum código foi informado.");
        }

        String digitos = entrada.replaceAll("[\\s.\\-]", "");

        if (!digitos.matches("\\d+")) {
            throw new EntradaInvalidaException(
                    "O código deve ter só números (pontos, traços e espaços são ignorados).");
        }
        return digitos;
    }

    private static Boleto montar(Origem origem, String barras, String linha, LocalDate hoje) {
        int fator = Integer.parseInt(barras.substring(5, 9));
        long centavos = Long.parseLong(barras.substring(9, 19));

        return new Boleto(
                origem,
                barras,
                linha,
                barras.substring(0, 3),
                barras.charAt(3),
                barras.charAt(4) - '0',
                fator,
                FatorVencimento.paraData(fator, hoje),
                BigDecimal.valueOf(centavos, 2), // 25000 centavos → 250.00
                barras.substring(19, 44));
    }

    /**
     * A linha digitável é o código de barras "embaralhado" em 5 campos, com DVs extras:
     * <pre>
     * campo 1: banco + moeda + 5 primeiros do campo livre + DV   (posições  1-10)
     * campo 2: próximos 10 do campo livre + DV                    (posições 11-21)
     * campo 3: últimos 10 do campo livre + DV                     (posições 22-32)
     * campo 4: DV geral                                           (posição  33)
     * campo 5: fator de vencimento + valor                        (posições 34-47)
     * </pre>
     */
    public static String linhaParaCodigoDeBarras(String linha) {
        return linha.substring(0, 4)      // banco + moeda
                + linha.charAt(32)        // DV geral
                + linha.substring(33, 47) // fator + valor
                + linha.substring(4, 9)   // campo livre, parte 1
                + linha.substring(10, 20) // campo livre, parte 2
                + linha.substring(21, 31);// campo livre, parte 3
    }

    /** Caminho inverso: código de barras → linha digitável (calculando os DVs dos campos). */
    public static String codigoDeBarrasParaLinha(String barras) {
        String campo1 = barras.substring(0, 4) + barras.substring(19, 24);
        String campo2 = barras.substring(24, 34);
        String campo3 = barras.substring(34, 44);

        return campo1 + DigitoVerificador.modulo10(campo1)
                + campo2 + DigitoVerificador.modulo10(campo2)
                + campo3 + DigitoVerificador.modulo10(campo3)
                + barras.charAt(4)
                + barras.substring(5, 19);
    }
}
