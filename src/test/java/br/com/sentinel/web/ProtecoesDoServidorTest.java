package br.com.sentinel.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static br.com.sentinel.Exemplos.HOJE;
import static br.com.sentinel.Exemplos.VALIDO_LINHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa as proteções contra abuso: limite de requisições e conexões lentas. */
class ProtecoesDoServidorTest {

    private final HttpClient cliente = HttpClient.newBuilder()
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private ServidorWeb servidor;
    private final List<Socket> conexoesLentas = new ArrayList<>();

    @AfterEach
    void limpar() throws IOException {
        for (Socket s : conexoesLentas) {
            s.close();
        }
        if (servidor != null) {
            servidor.parar();
        }
    }

    private ServidorWeb subir(boolean atrasDeProxy, int limitePorIp) throws IOException {
        servidor = new ServidorWeb(new ConfiguracaoServidor(0, atrasDeProxy, limitePorIp, 1000), () -> HOJE);
        servidor.iniciar();
        return servidor;
    }

    private HttpResponse<String> analisar(String encaminhadoPor) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + servidor.porta() + "/api/analisar"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("codigo=" + VALIDO_LINHA));

        if (encaminhadoPor != null) {
            req.header("X-Forwarded-For", encaminhadoPor);
        }
        return cliente.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void passouDoLimiteRecebe429ComRetryAfter() throws Exception {
        subir(false, 3);

        for (int i = 0; i < 3; i++) {
            assertEquals(200, analisar(null).statusCode());
        }

        HttpResponse<String> bloqueada = analisar(null);
        assertEquals(429, bloqueada.statusCode());
        assertTrue(bloqueada.body().contains("Espere um minuto"));
        assertTrue(bloqueada.headers().firstValue("Retry-After").isPresent());
    }

    @Test
    void atrasDeProxyContaPeloIpRealDoCliente() throws Exception {
        subir(true, 1);

        assertEquals(200, analisar("200.1.1.1").statusCode());
        assertEquals(429, analisar("200.1.1.1").statusCode());
        assertEquals(200, analisar("200.2.2.2, 10.0.0.1").statusCode()); // outro cliente
    }

    @Test
    void semProxyIgnoraXForwardedForFalso() throws Exception {
        // fora de um proxy, o cabeçalho é ignorado: não dá para burlar o limite mentindo o IP
        subir(false, 1);

        assertEquals(200, analisar("1.1.1.1").statusCode());
        assertEquals(429, analisar("2.2.2.2").statusCode());
    }

    @Test
    void enviaHsts() throws Exception {
        subir(false, 10);
        assertTrue(analisar(null).headers().firstValue("Strict-Transport-Security").orElse("").contains("max-age="));
    }

    @Test
    void fechaConexaoQueNuncaTerminaDeMandarOCabecalho() throws Exception {
        subir(false, 10);
        Socket lenta = abrirConexaoLenta("POST /api/analisar HTTP/1.1\r\nHost: localhost\r\n"); // falta a linha em branco

        long inicio = System.nanoTime();
        assertTrue(servidorFechou(lenta), "o servidor deveria fechar a conexão lenta");
        long segundos = Duration.ofNanos(System.nanoTime() - inicio).toSeconds();
        assertTrue(segundos <= 6, "demorou " + segundos + " s para fechar");
    }

    @Test
    void fechaConexaoQuePrometeCorpoENuncaManda() throws Exception {
        subir(false, 10);
        Socket lenta = abrirConexaoLenta("POST /api/analisar HTTP/1.1\r\nHost: localhost\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: 100\r\n\r\ncodigo=1");

        assertTrue(servidorFechou(lenta), "o servidor deveria fechar a conexão lenta");
    }

    @Test
    void continuaAtendendoMesmoComMuitasConexoesLentas() throws Exception {
        // limite alto: as conexões lentas também contam como requisições do mesmo IP
        subir(false, 100);

        // mais conexões lentas do que atendentes: sem proteção, o servidor travaria
        for (int i = 0; i < ServidorWeb.ATENDENTES + 4; i++) {
            abrirConexaoLenta("POST /api/analisar HTTP/1.1\r\nHost: localhost\r\nContent-Length: 50\r\n\r\n");
        }

        Thread.sleep(4000); // tempo de o servidor derrubar as conexões lentas

        HttpResponse<String> normal = analisar(null);
        assertEquals(200, normal.statusCode(), normal.body());
    }

    // ---------------------------------------------------------------- ajudantes

    private Socket abrirConexaoLenta(String inicioDaRequisicao) throws IOException {
        Socket socket = new Socket("localhost", servidor.porta());
        OutputStream saida = socket.getOutputStream();
        saida.write(inicioDaRequisicao.getBytes(StandardCharsets.US_ASCII));
        saida.flush();
        conexoesLentas.add(socket);
        return socket;
    }

    /** Espera até 8 s: true se o servidor fechou a conexão (fim de stream ou erro de conexão). */
    private static boolean servidorFechou(Socket socket) throws IOException {
        socket.setSoTimeout(8000);
        InputStream entrada = socket.getInputStream();

        try {
            while (true) {
                int lido = entrada.read();
                if (lido == -1) {
                    return true;   // fechou "educadamente"
                }
                // se o servidor mandou alguma resposta (ex.: 400), continua lendo até ele fechar
            }
        } catch (SocketTimeoutException e) {
            return false;          // ficou pendurada: proteção NÃO funcionou
        } catch (IOException e) {
            return true;           // conexão resetada pelo servidor
        }
    }
}
