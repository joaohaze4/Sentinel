package br.com.sentinel;

import java.time.LocalDate;

/**
 * Boletos FICTÍCIOS usados nos testes e no README.
 *
 * <p>Foram gerados por um programa separado (em Python, com outra implementação dos cálculos),
 * para os testes não "conferirem o código contra ele mesmo".</p>
 */
public final class Exemplos {

    /** Data fixa usada como "hoje" nos testes, para eles não quebrarem com o passar do tempo. */
    public static final LocalDate HOJE = LocalDate.of(2026, 11, 15);

    /** Itaú, R$ 250,00, vence 10/12/2026. Tudo certo. */
    public static final String VALIDO_LINHA = "34191790011043510047391020150008716560000025000";
    public static final String VALIDO_BARRAS = "34197165600000250001790010435100479102015000";

    /** O válido com UM dígito trocado no campo 2 (como um golpista "amador" faria). */
    public static final String ADULTERADO_LINHA = "34191790011073510047391020150008716560000025000";

    /** Golpe "bem feito": mesmo valor e vencimento, mas para o Bradesco, com todos os dígitos recalculados. */
    public static final String BANCO_TROCADO_LINHA = "23791790011043510047391020150008216560000025000";

    /** Bradesco, R$ 1.899,90, venceu em 01/10/2026. */
    public static final String VENCIDO_LINHA = "23790928161234567890312345678903215860000189990";

    /** Banco do Brasil, sem valor no código, vence 20/12/2026. */
    public static final String SEM_VALOR_LINHA = "00190000090123456700400001234178916660000000000";

    /** Código de banco 999 (não existe na lista). */
    public static final String BANCO_DESCONHECIDO_LINHA = "99991790061043510047391020150008516560000050000";

    /** Caixa, vencimento em 01/03/2028 (mais de 1 ano à frente). */
    public static final String VENCIMENTO_LONGE_LINHA = "10492400035100000000301234567897521030000009900";

    /** Santander, sem vencimento (fator 0000), R$ 150,50. */
    public static final String SEM_VENCIMENTO_LINHA = "03399123475670000001323456701012200000000015050";

    /** Moeda 5 em vez de 9 (dígitos recalculados para isolar só esse problema). */
    public static final String MOEDA_ERRADA_LINHA = "34151790051043510047391020150008116560000025000";

    // ----------------------------------------------------- contas de consumo (arrecadação)

    /** Energia (segmento 3), módulo 10, R$ 187,52, empresa 0048. */
    public static final String LUZ_LINHA = "836500000010875200480007000000012344567890123456";
    public static final String LUZ_BARRAS = "83650000001875200480000000000123456789012345";

    /** A conta de luz com UM dígito trocado no bloco 2. */
    public static final String LUZ_ADULTERADA_LINHA = "836500000010879200480007000000012344567890123456";

    /** Água (segmento 2), módulo 11, R$ 94,30, empresa 0102. */
    public static final String AGUA_LINHA = "828500000000943001022025612100000004004567890000";

    /** Telefone (segmento 4), R$ 187,52, empresa 0221. */
    public static final String TELEFONE_LINHA = "846300000011875202210006000000012344567890123456";

    /** Carnê (segmento 6): empresa identificada pela raiz do CNPJ 12.345.678. */
    public static final String CNPJ_LINHA = "866600000057499012345678800000000003987654321007";

    /** Valor de referência (3ª posição = 7), não em reais. */
    public static final String REFERENCIA_LINHA = "847300000001100002210001000000000000005555111128";

    /** Sem valor no código. */
    public static final String CONTA_SEM_VALOR_LINHA = "816500000004000007770001000000000000000000123455";

    /** 3ª posição = 5 (não existe). */
    public static final String IDENTIFICACAO_INVALIDA_LINHA = "835600000013875200480009000000012343567890123457";

    /** Segmento 0 (não existe). */
    public static final String SEGMENTO_INVALIDO_LINHA = "806800000010875200480007000000012344567890123456";

    private Exemplos() {
    }
}
