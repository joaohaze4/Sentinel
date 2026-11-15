package br.com.sentinel.analise;

import java.util.List;

/** Resultado final, decidido pelo alerta MAIS GRAVE encontrado. */
public enum Classificacao {

    SEM_ALERTAS("Nenhum sinal de fraude encontrado nos números do boleto."),
    BAIXO_RISCO("Detalhes para conferir, mas nada grave."),
    ATENCAO("Confira com o emissor antes de pagar."),
    ALTO_RISCO("NÃO pague antes de confirmar o boleto direto com quem cobrou (site, app ou telefone oficial).");

    private final String recomendacao;

    Classificacao(String recomendacao) {
        this.recomendacao = recomendacao;
    }

    public String recomendacao() {
        return recomendacao;
    }

    public static Classificacao de(List<Alerta> alertas) {
        boolean temAlto = alertas.stream().anyMatch(a -> a.nivel() == Nivel.ALTO);
        boolean temMedio = alertas.stream().anyMatch(a -> a.nivel() == Nivel.MEDIO);

        if (temAlto) {
            return ALTO_RISCO;
        }
        if (temMedio) {
            return ATENCAO;
        }
        if (!alertas.isEmpty()) {
            return BAIXO_RISCO;
        }
        return SEM_ALERTAS;
    }
}
