package br.com.sentinel.boleto;

import java.util.Map;
import java.util.Optional;

/**
 * Lista (resumida) de bancos pelo código de compensação.
 *
 * <p>Um código fora da lista não prova fraude: pode ser um banco pequeno ou cooperativa
 * que ainda não foi cadastrada aqui. Por isso vira alerta de nível médio, não alto.</p>
 */
public final class Bancos {

    private static final Map<String, String> NOMES = Map.ofEntries(
            Map.entry("001", "Banco do Brasil"),
            Map.entry("003", "Banco da Amazônia"),
            Map.entry("004", "Banco do Nordeste"),
            Map.entry("021", "Banestes"),
            Map.entry("033", "Santander"),
            Map.entry("037", "Banpará"),
            Map.entry("041", "Banrisul"),
            Map.entry("047", "Banese"),
            Map.entry("070", "BRB - Banco de Brasília"),
            Map.entry("077", "Banco Inter"),
            Map.entry("085", "Ailos"),
            Map.entry("104", "Caixa Econômica Federal"),
            Map.entry("136", "Unicred"),
            Map.entry("208", "BTG Pactual"),
            Map.entry("212", "Banco Original"),
            Map.entry("237", "Bradesco"),
            Map.entry("260", "Nu Pagamentos (Nubank)"),
            Map.entry("290", "PagBank"),
            Map.entry("323", "Mercado Pago"),
            Map.entry("336", "C6 Bank"),
            Map.entry("341", "Itaú Unibanco"),
            Map.entry("380", "PicPay"),
            Map.entry("389", "Banco Mercantil do Brasil"),
            Map.entry("422", "Banco Safra"),
            Map.entry("623", "Banco Pan"),
            Map.entry("655", "Banco Votorantim (BV)"),
            Map.entry("707", "Banco Daycoval"),
            Map.entry("745", "Citibank"),
            Map.entry("748", "Sicredi"),
            Map.entry("756", "Sicoob")
    );

    private Bancos() {
    }

    public static Optional<String> nome(String codigo) {
        return Optional.ofNullable(NOMES.get(codigo));
    }

    /** "341 (Itaú Unibanco)" ou "999 (banco não identificado)". */
    public static String descricao(String codigo) {
        return codigo + " (" + nome(codigo).orElse("banco não identificado") + ")";
    }
}
