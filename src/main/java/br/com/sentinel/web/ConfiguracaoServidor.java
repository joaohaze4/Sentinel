package br.com.sentinel.web;

/**
 * Ajustes do servidor web.
 *
 * @param porta                  porta TCP (0 = qualquer livre, usado nos testes)
 * @param atrasDeProxy           {@code true} quando roda atrás de um proxy/balanceador (como no Render).
 *                               Aí o IP real do cliente vem no cabeçalho X-Forwarded-For.
 * @param limitePorIpPorMinuto   análises por minuto que um mesmo IP pode fazer
 * @param limiteGlobalPorMinuto  análises por minuto somando todo mundo
 */
public record ConfiguracaoServidor(int porta, boolean atrasDeProxy,
                                   int limitePorIpPorMinuto, int limiteGlobalPorMinuto) {

    public static final int LIMITE_POR_IP = 30;
    public static final int LIMITE_GLOBAL = 600;

    /**
     * Configuração padrão. Considera que está atrás de proxy quando a variável de ambiente
     * SENTINEL_ATRAS_DE_PROXY=true (definida no render.yaml) ou RENDER (que o Render define sozinho).
     */
    public static ConfiguracaoServidor padrao(int porta) {
        boolean atrasDeProxy = "true".equalsIgnoreCase(System.getenv("SENTINEL_ATRAS_DE_PROXY"))
                || System.getenv("RENDER") != null;

        return new ConfiguracaoServidor(porta, atrasDeProxy, LIMITE_POR_IP, LIMITE_GLOBAL);
    }
}
