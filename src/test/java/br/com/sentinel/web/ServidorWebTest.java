package br.com.sentinel.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static br.com.sentinel.Exemplos.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobe o servidor de verdade numa porta livre e faz requisições HTTP nele. */
class ServidorWebTest {

    private static ServidorWeb servidor;
    private static HttpClient cliente;
    private static String base;

    @BeforeAll
    static void subirServidor() throws IOException {
        servidor = new ServidorWeb(0, () -> HOJE); // porta 0 = o sistema escolhe uma livre
        servidor.iniciar();
        base = "http://localhost:" + servidor.porta();
        cliente = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    }

    @AfterAll
    static void derrubarServidor() {
        servidor.parar();
    }

    private HttpResponse<String> post(String caminho, String corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(base + caminho))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(base + caminho)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void analisaBoletoValido() throws Exception {
        HttpResponse<String> r = post("/api/analisar", "codigo=" + VALIDO_LINHA);

        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(r.body().contains("\"classificacao\":\"SEM_ALERTAS\""));
        assertTrue(r.body().contains("\"bancoNome\":\"Itaú Unibanco\""));
        assertTrue(r.body().contains("\"valor\":\"250.00\""));
        assertTrue(r.body().contains("\"diasParaVencer\":25"));
        assertTrue(r.body().contains("\"alertas\":[]"));
    }

    @Test
    void pegaGolpeComBancoEsperado() throws Exception {
        HttpResponse<String> r = post("/api/analisar",
                "codigo=" + BANCO_TROCADO_LINHA + "&banco=341&valor=250%2C00&vencimento=");

        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"classificacao\":\"ALTO_RISCO\""));
        assertTrue(r.body().contains("Banco diferente do esperado"));
    }

    @Test
    void aceitaDataDoCampoDeData() throws Exception {
        // <input type="date"> envia AAAA-MM-DD
        HttpResponse<String> r = post("/api/analisar", "codigo=" + VALIDO_LINHA + "&vencimento=2026-12-05");

        assertTrue(r.body().contains("Vencimento diferente do esperado"));
    }

    @Test
    void codigoInvalidoDa400ComMensagem() throws Exception {
        HttpResponse<String> r = post("/api/analisar", "codigo=123");

        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"erro\""));
        assertTrue(r.body().contains("3 dígitos"));
    }

    @Test
    void semCodigoDa400() throws Exception {
        assertEquals(400, post("/api/analisar", "").statusCode());
    }

    @Test
    void getNaApiDa405() throws Exception {
        HttpResponse<String> r = get("/api/analisar");

        assertEquals(405, r.statusCode());
        assertEquals("POST", r.headers().firstValue("Allow").orElse(""));
    }

    @Test
    void corpoGrandeDemaisDa413() throws Exception {
        String enorme = "codigo=" + "1".repeat(ServidorWeb.TAMANHO_MAXIMO_REQUISICAO);
        assertEquals(413, post("/api/analisar", enorme).statusCode());
    }

    @Test
    void entregaAPaginaComCabecalhosDeSeguranca() throws Exception {
        HttpResponse<String> r = get("/");

        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("<title>Sentinel"));
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'self'"));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertTrue(r.headers().firstValue("Strict-Transport-Security").isPresent());
    }

    @Test
    void entregaCssEJs() throws Exception {
        assertTrue(get("/estilo.css").headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
        assertTrue(get("/app.js").headers().firstValue("Content-Type").orElse("").startsWith("text/javascript"));
    }

    @Test
    void naoDeixaPassearPorOutrosArquivos() throws Exception {
        assertEquals(404, get("/../build.gradle").statusCode());
        assertEquals(404, get("/web/index.html").statusCode());
        assertEquals(404, get("/br/com/sentinel/Main.class").statusCode());
    }

    @Test
    void saude() throws Exception {
        HttpResponse<String> r = get("/api/saude");

        assertEquals(200, r.statusCode());
        assertEquals("{\"status\":\"ok\"}", r.body());
    }
}
