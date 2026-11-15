package br.com.sentinel.analise;

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
 */
public record DadosEsperados(BigDecimal valor, String codigoBanco, LocalDate vencimento) {

    public static DadosEsperados nenhum() {
        return new DadosEsperados(null, null, null);
    }
}
