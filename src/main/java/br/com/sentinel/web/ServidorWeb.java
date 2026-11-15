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
 */
public class ServidorWeb {

    /** Um boleto tem no máximo ~60 caracteres; 4 KB sobra e impede abuso com corpos gigantes. */
    static final int TAMANHO_MAXIMO_REQUISICAO = 4096;

    private record ArquivoEstatico(String recurso, String tipo) {
    }

    /**
     * Lista fechada de arquivos que podem ser baixados. Nunca montamos o caminho a partir
     * do que o usuário pediu, então não tem como pedir "../../senhas.txt" (path traversal).
     */
    private static final Map<String, ArquivoEstatico> ARQUIVOS = Map.of(
            "/", new ArquivoEstatico("/web/index.html", "text/html; charset=utf-8"),
            "/index.html", new ArquivoEstatico("/web/index.html", "text/html; charset=utf-8"),
            "/estilo.css", new ArquivoEstatico("/web/estilo.css", "text/css; charset=utf-8"),
            "/app.js", new ArquivoEstatico("/web/app.js", "text/javascript; charset=utf-8"));

    private final HttpServer servidor;
    private final ExecutorService executor;
    private final AnalisadorDeRisco analisador = new AnalisadorDeRisco();
    private final Supplier<LocalDate> relogio;

    public ServidorWeb(int porta) throws IOException {
        this(porta, LocalDate::now);
    }

    /** @param relogio de onde vem a data de "hoje" (nos testes, uma data fixa) */
    public ServidorWeb(int porta, Supplier<LocalDate> relogio) throws IOException {
        this.relogio = relogio;
        this.servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        this.executor = Executors.newFixedThreadPool(4);

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
        ArquivoEstatico arquivo = ARQUIVOS.get(troca.getRequestURI().getPath());

        if (!"GET".equals(metodo) && !"HEAD".equals(metodo)) {
            troca.getResponseHeaders().set("Allow", "GET, HEAD");
            responder(troca, 405, "Método não permitido.", "text/plain; charset=utf-8");
            return;
        }

        if (arquivo == null) {
            responder(troca, 404, "Página não encontrada.", "text/plain; charset=utf-8");
            return;
        }

        try (InputStream entrada = ServidorWeb.class.getResourceAsStream(arquivo.recurso())) {
            if (entrada == null) {
                responder(troca, 404, "Página não encontrada.", "text/plain; charset=utf-8");
                return;
            }
            responder(troca, 200, entrada.readAllBytes(), arquivo.tipo());
        }
    }

    // ------------------------------------------------------------------ utilitários

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
