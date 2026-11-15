package br.com.sentinel.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void escapaAspasBarrasEQuebrasDeLinha() {
        assertEquals("\"diz \\\"oi\\\"\\\\ \\n fim\"", Json.texto("diz \"oi\"\\ \n fim"));
    }

    @Test
    void escapaCaracteresDeControle() {
        assertEquals("\"a\\u0001b\"", Json.texto("a\u0001b"));
    }

    @Test
    void nuloViraNullSemAspas() {
        assertEquals("null", Json.texto(null));
    }

    @Test
    void mantemAcentos() {
        assertEquals("\"Itaú\"", Json.texto("Itaú"));
    }
}
