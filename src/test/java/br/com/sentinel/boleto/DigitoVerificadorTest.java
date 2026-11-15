package br.com.sentinel.boleto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigitoVerificadorTest {

    @ParameterizedTest(name = "módulo 10 de {0} = {1}")
    @CsvSource({
            "341917900, 1",
            "0104351004, 7",
            "7910201500, 9",
            "123456789, 7",
            "0000000000, 0"
    })
    void calculaModulo10(String numeros, int esperado) {
        assertEquals(esperado, DigitoVerificador.modulo10(numeros));
    }

    @Test
    void calculaModulo11DoCodigoDeBarras() {
        // código de barras válido sem o 5º dígito (que é o próprio DV)
        assertEquals(8, DigitoVerificador.modulo11("3419187770000025000179001043510047910201500"));
    }

    @Test
    void modulo11TrocaResultado10ou11Por1() {
        // 43 zeros: soma 0 → 11 - 0 = 11 → vira 1
        assertEquals(1, DigitoVerificador.modulo11("0".repeat(43)));
    }
}
