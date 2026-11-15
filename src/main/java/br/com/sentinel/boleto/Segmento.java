package br.com.sentinel.boleto;

import java.util.Optional;

/** Tipo de conta de consumo (2ª posição do código), conforme a FEBRABAN. */
public enum Segmento {

    PREFEITURAS('1', "Prefeituras (IPTU e taxas)"),
    SANEAMENTO('2', "Água e esgoto"),
    ENERGIA_E_GAS('3', "Energia elétrica e gás"),
    TELECOMUNICACOES('4', "Telefone, internet e TV"),
    ORGAOS_GOVERNAMENTAIS('5', "Órgãos do governo"),
    EMPRESAS_CNPJ('6', "Carnês e outras empresas (identificadas por CNPJ)"),
    MULTAS_DE_TRANSITO('7', "Multas de trânsito"),
    USO_DO_BANCO('9', "Uso exclusivo do banco");

    private final char codigo;
    private final String descricao;

    Segmento(char codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public char codigo() {
        return codigo;
    }

    public String descricao() {
        return descricao;
    }

    /** '3' → ENERGIA_E_GAS. Vazio quando o código não existe (0 e 8 não são usados). */
    public static Optional<Segmento> de(char codigo) {
        for (Segmento s : values()) {
            if (s.codigo == codigo) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
