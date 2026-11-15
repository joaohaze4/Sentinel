package br.com.sentinel.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioTest {

    @Test
    void lePares() {
        Map<String, String> campos = Formulario.ler("codigo=34191.79001+10435&valor=250%2C00&banco=341");

        assertEquals("34191.79001 10435", campos.get("codigo")); // + vira espaço
        assertEquals("250,00", campos.get("valor"));              // %2C vira vírgula
        assertEquals("341", campos.get("banco"));
    }

    @Test
    void aceitaCampoVazioESemIgual() {
        Map<String, String> campos = Formulario.ler("valor=&banco&&codigo=1");

        assertEquals("", campos.get("valor"));
        assertEquals("", campos.get("banco"));
        assertEquals("1", campos.get("codigo"));
    }

    @Test
    void corpoVazioDaMapaVazio() {
        assertTrue(Formulario.ler("").isEmpty());
        assertTrue(Formulario.ler(null).isEmpty());
    }
}
