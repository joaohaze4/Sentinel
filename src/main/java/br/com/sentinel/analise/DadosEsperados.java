package br.com.sentinel.analise;

import br.com.sentinel.boleto.Segmento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que o usuário ESPERA pagar, tirado da fatura, do contrato ou do cadastro do fornecedor.
 *
 * <p>É aqui que mora a parte mais forte da análise: o golpe mais comum troca o código
 * de barras por outro válido, de outro banco. Matematicamente o boleto falso é perfeito,
 * mas o banco (ou o valor) não bate com o que o usuário esperava.</p>
 *
 * <p>Todos os campos são opcionais: {@code null} = não informado.</p>
 *
 * @param valor       valor esperado (boleto e conta de consumo)
 * @param codigoBanco banco esperado (só boleto)
 * @param vencimento  vencimento esperado (só boleto; conta de consumo não traz vencimento no código)
 * @param segmento    tipo de conta esperado, ex.: energia (só conta de consumo)
 * @param empresa     código da empresa da conta anterior (só conta de consumo)
 */
public record DadosEsperados(BigDecimal valor, String codigoBanco, LocalDate vencimento,
                             Segmento segmento, String empresa) {

    /** Atalho para quem só informa os dados de boleto. */
    public DadosEsperados(BigDecimal valor, String codigoBanco, LocalDate vencimento) {
        this(valor, codigoBanco, vencimento, null, null);
    }

    public static DadosEsperados nenhum() {
        return new DadosEsperados(null, null, null, null, null);
    }
}
