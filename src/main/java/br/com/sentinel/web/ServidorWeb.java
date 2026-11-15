package br.com.sentinel.web;

import br.com.sentinel.analise.AnalisadorDeRisco;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Resultado;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.EntradaInvalidaException;
import br.com.sentinel.boleto.LeitorDeBoleto;
import br.com.sentinel.cli.Argumentos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Servidor web do Sentinel: entrega a página (front-end) e a API que faz a análise (back-end).
 *
 * <p>Usa o {@link HttpServer} que já vem no Java, sem frameworks. Rotas:</p>
 * <pre>
 * GET  /               página do Sentinel
 * GET  /estilo.css     estilos
 * GET  /app.js         lógica da página
 * POST /api/analisar   recebe o formulário e devolve a análise em JSON
 * GET  /api/saude      responde {"status":"ok"} (usado por serviços de hospedagem)
 * </pre>
 *
 * <p>Proteções: limite de requisições por IP, tempo máximo por requisição (contra conexões
 * lentas de propósito), lista fechada de arquivos, limite de tamanho e cabeçalhos de segurança.</p>
 */
public class ServidorWeb {

    /** Um boleto tem no máximo ~60 caracteres; 4 KB sobra e impede abuso com corpos gigantes. */
    static final int TAMANHO_MAXIMO_REQUISICAO = 4096;

    /** Quantas requisições podem ser atendidas ao mesmo tempo. */
    static final int ATENDENTES = 16;

    /*
     * Proteção contra "slowloris": o atacante abre conexões e manda os dados bem devagar
     * (ou nunca termina), prendendo os atendentes do servidor até ninguém mais ser atendido.
     *
     * O HttpServer do Java lê estes ajustes UMA vez, quando é carregado. Por padrão eles
     * são "infinito". Aqui limitamos a 10 segundos para receber a requisição e 10 para
     * mandar a resposta; passou disso, a conexão é fechada.
     * (Só definimos se ninguém definiu antes, para os testes poderem usar valores menores.)
     */
    static {
        definirSeAusente("sun.net.httpserver.maxReqTime", "10");
        definirSeAusente("sun.net.httpserver.maxRspTime", "10");
    }

    private static void definirSeAusente(String propriedade, String valor) {
        if (System.getProperty(propriedade) == null) {
            System.setProperty(propriedade, valor);
        }
    }

    private record ArquivoEstatico(byte[] conteudo, String tipo) {
    }

    /**
     * Lista fechada de arquivos que podem ser baixados. Nunca montamos o caminho a partir
     * do que o usuário pediu, então não tem como pedir "../../senhas.txt" (path traversal).
     */
    private final Map<String, ArquivoEstatico> arquivos;

    private final HttpServer servidor;
    private final ExecutorService executor;
    private final AnalisadorDeRisco analisador = new AnalisadorDeRisco();
    private final LimitadorDeRequisicoes limitador;
    private final boolean atrasDeProxy;
    private final Supplier<LocalDate> relogio;

    public ServidorWeb(int porta) throws IOException {
        this(ConfiguracaoServidor.padrao(porta), LocalDate::now);
    }

    /** @param relogio de onde vem a data de "hoje" (nos testes, uma data fixa) */
    public ServidorWeb(int porta, Supplier<LocalDate> relogio) throws IOException {
        this(new ConfiguracaoServidor(porta, false,
                ConfiguracaoServidor.LIMITE_POR_IP, ConfiguracaoServidor.LIMITE_GLOBAL), relogio);
    }

    public ServidorWeb(ConfiguracaoServidor config, Supplier<LocalDate> relogio) throws IOException {
        this.relogio = relogio;
        this.atrasDeProxy = config.atrasDeProxy();
        this.limitador = new LimitadorDeRequisicoes(config.limitePorIpPorMinuto(), config.limiteGlobalPorMinuto());

        // os arquivos são lidos uma vez só, na inicialização, e ficam na memória
        ArquivoEstatico pagina = carregar("/web/index.html", "text/html; charset=utf-8");
        this.arquivos = Map.of(
                "/", pagina,
                "/index.html", pagina,
                "/estilo.css", carregar("/web/estilo.css", "text/css; charset=utf-8"),
                "/app.js", carregar("/web/app.js", "text/javascript; charset=utf-8"));

        this.servidor = HttpServer.create(new InetSocketAddress(config.porta()), 0);
        this.executor = Executors.newFixedThreadPool(ATENDENTES);

        servidor.createContext("/api/analisar", this::analisar);
        servidor.createContext("/api/saude", this::saude);
        servidor.createContext("/", this::arquivoEstatico);
        servidor.setExecutor(executor);
    }

    public void iniciar() {
        servidor.start();
    }

    public void parar() {
        servidor.stop(0);
        executor.shutdownNow();
    }

    /** Porta em que o servidor está ouvindo (útil quando se pede a porta 0 = "qualquer livre"). */
    public int porta() {
        return servidor.getAddress().getPort();
    }

    // ------------------------------------------------------------------ rotas

    private void analisar(HttpExchange troca) throws IOException {
        try {
            if (!troca.getRequestURI().getPath().equals("/api/analisar")) {
                responder(troca, 404, Json.erro("Endereço não encontrado."), "application/json; charset=utf-8");
                return;
            }

            if (!"POST".equals(troca.getRequestMethod())) {
                troca.getResponseHeaders().set("Allow", "POST");
                responder(troca, 405, Json.erro("Use o método POST."), "application/json; charset=utf-8");
                return;
            }

            if (!limitador.permitir(ipDoCliente(troca))) {
                troca.getResponseHeaders().set("Retry-After", String.valueOf(limitador.segundosParaLiberar()));
                responder(troca, 429, Json.erro("Muitas análises em pouco tempo. Espere um minuto e tente de novo."),
                        "application/json; charset=utf-8");
                return;
            }

            Map<String, String> campos = Formulario.ler(lerCorpo(troca));
            LocalDate hoje = relogio.get();

            DadosEsperados esperado = new DadosEsperados(
                    vazio(campos.get("valor")) ? null : Argumentos.lerValor(campos.get("valor")),
                    vazio(campos.get("banco")) ? null : Argumentos.lerBanco(campos.get("banco")),
                    vazio(campos.get("vencimento")) ? null : Argumentos.lerData(campos.get("vencimento")));

            Boleto boleto = LeitorDeBoleto.ler(campos.get("codigo"), hoje);
            Resultado resultado = analisador.analisar(boleto, esperado, hoje);

            responder(troca, 200, RespostaJson.de(resultado, hoje), "application/json; charset=utf-8");

        } catch (EntradaInvalidaException e) {
            responder(troca, 400, Json.erro(e.getMessage()), "application/json; charset=utf-8");
        } catch (RequisicaoGrandeException e) {
            responder(troca, 413, Json.erro("Requisição grande demais."), "application/json; charset=utf-8");
        } catch (RuntimeException e) {
            System.err.println("Erro inesperado: " + e);
            responder(troca, 500, Json.erro("Erro interno no servidor."), "application/json; charset=utf-8");
        }
    }

    private void saude(HttpExchange troca) throws IOException {
        responder(troca, 200, "{\"status\":\"ok\"}", "application/json; charset=utf-8");
    }

    private void arquivoEstatico(HttpExchange troca) throws IOException {
        String metodo = troca.getRequestMethod();
        ArquivoEstatico arquivo = arquivos.get(troca.getRequestURI().getPath());

        if (!"GET".equals(metodo) && !"HEAD".equals(metodo)) {
            troca.getResponseHeaders().set("Allow", "GET, HEAD");
            responder(troca, 405, "Método não permitido.", "text/plain; charset=utf-8");
            return;
        }

        if (arquivo == null) {
            responder(troca, 404, "Página não encontrada.", "text/plain; charset=utf-8");
            return;
        }

        responder(troca, 200, arquivo.conteudo(), arquivo.tipo());
    }

    // ------------------------------------------------------------------ utilitários

    private static ArquivoEstatico carregar(String recurso, String tipo) throws IOException {
        try (InputStream entrada = ServidorWeb.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IOException("Arquivo do front-end não encontrado: " + recurso);
            }
            return new ArquivoEstatico(entrada.readAllBytes(), tipo);
        }
    }

    /**
     * IP de quem fez a requisição.
     *
     * <p>Atrás de um proxy (como no Render), todas as conexões chegam "vindas" do proxy.
     * O IP real vem no cabeçalho X-Forwarded-For. Ele pode ser falsificado pelo cliente,
     * por isso só é usado quando configurado, e o limite GLOBAL continua protegendo
     * mesmo se alguém mentir o IP.</p>
     */
    private String ipDoCliente(HttpExchange troca) {
        if (atrasDeProxy) {
            String encaminhado = troca.getRequestHeaders().getFirst("X-Forwarded-For");

            if (encaminhado != null && !encaminhado.isBlank()) {
                return encaminhado.split(",")[0].trim();
            }
        }
        return troca.getRemoteAddress().getAddress().getHostAddress();
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String lerCorpo(HttpExchange troca) throws IOException {
        byte[] dados = troca.getRequestBody().readNBytes(TAMANHO_MAXIMO_REQUISICAO + 1);

        if (dados.length > TAMANHO_MAXIMO_REQUISICAO) {
            throw new RequisicaoGrandeException();
        }
        return new String(dados, StandardCharsets.UTF_8);
    }

    private static void responder(HttpExchange troca, int status, String corpo, String tipo) throws IOException {
        responder(troca, status, corpo.getBytes(StandardCharsets.UTF_8), tipo);
    }

    private static void responder(HttpExchange troca, int status, byte[] corpo, String tipo) throws IOException {
        var cabecalhos = troca.getResponseHeaders();
        cabecalhos.set("Content-Type", tipo);
        cabecalhos.set("X-Content-Type-Options", "nosniff");      // navegador não "adivinha" o tipo do arquivo
        cabecalhos.set("Referrer-Policy", "no-referrer");
        cabecalhos.set("Cache-Control", "no-store");               // dados de boleto não ficam em cache
        cabecalhos.set("Content-Security-Policy",                  // só carrega scripts/estilos do próprio site
                "default-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        cabecalhos.set("Strict-Transport-Security",                // HSTS: navegador passa a usar só HTTPS
                "max-age=31536000");                               // (ignorado em http://localhost, como deve ser)

        boolean head = "HEAD".equals(troca.getRequestMethod());
        troca.sendResponseHeaders(status, head ? -1 : corpo.length);

        if (!head) {
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        }
        troca.close();
    }

    /** Corpo da requisição maior que o permitido. */
    private static class RequisicaoGrandeException extends RuntimeException {
    }
}
