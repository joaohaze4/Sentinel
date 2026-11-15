package br.com.sentinel.analise;

import br.com.sentinel.boleto.LeitorDeDocumento;
import br.com.sentinel.boleto.Segmento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.sentinel.Exemplos.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnaliseContaDeConsumoTest {

    private final AnalisadorDeRisco analisador = new AnalisadorDeRisco();

    private Resultado analisar(String codigo, DadosEsperados esperado) {
        return analisador.analisar(LeitorDeDocumento.ler(codigo, HOJE), esperado, HOJE);
    }

    private Resultado analisar(String codigo) {
        return analisar(codigo, DadosEsperados.nenhum());
    }

    private boolean temAlerta(Resultado r, String trecho) {
        return r.alertas().stream().anyMatch(a -> a.titulo().contains(trecho));
    }

    @Test
    void contasValidasNaoTemAlertas() {
        assertEquals(Classificacao.SEM_ALERTAS, analisar(LUZ_LINHA).classificacao());       // módulo 10
        assertEquals(Classificacao.SEM_ALERTAS, analisar(LUZ_BARRAS).classificacao());
        assertEquals(Classificacao.SEM_ALERTAS, analisar(AGUA_LINHA).classificacao());      // módulo 11
        assertEquals(Classificacao.SEM_ALERTAS, analisar(CNPJ_LINHA).classificacao());      // segmento 6
        assertEquals(Classificacao.SEM_ALERTAS, analisar(TELEFONE_LINHA).classificacao());
    }

    @Test
    void contaAdulteradaEAltoRisco() {
        Resultado r = analisar(LUZ_ADULTERADA_LINHA);

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "bloco 2"));
        assertTrue(temAlerta(r, "verificador geral"));
    }

    @Test
    void identificacaoDeValorInvalidaEAltoRisco() {
        Resultado r = analisar(IDENTIFICACAO_INVALIDA_LINHA);

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Código de valor inválido"));
    }

    @Test
    void segmentoInexistenteEAltoRisco() {
        Resultado r = analisar(SEGMENTO_INVALIDO_LINHA);

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Segmento 0 não existe"));
    }

    @Test
    void valorDeReferenciaEBaixoRisco() {
        Resultado r = analisar(REFERENCIA_LINHA);

        assertEquals(Classificacao.BAIXO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "moeda de referência"));
    }

    @Test
    void contaSemValorEBaixoRisco() {
        assertTrue(temAlerta(analisar(CONTA_SEM_VALOR_LINHA), "Sem valor"));
    }

    @Test
    void valorDiferenteDoEsperado() {
        Resultado r = analisar(LUZ_LINHA, new DadosEsperados(new BigDecimal("150.00"), null, null));

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Valor diferente"));
    }

    @Test
    void tipoDeContaDiferenteDoEsperado() {
        // conta de TELEFONE, mas a pessoa acha que é de luz
        Resultado r = analisar(TELEFONE_LINHA,
                new DadosEsperados(null, null, null, Segmento.ENERGIA_E_GAS, null));

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Tipo de conta diferente"));
    }

    @Test
    void empresaDiferenteDaContaAnterior() {
        Resultado r = analisar(LUZ_LINHA, new DadosEsperados(null, null, null, null, "0099"));

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Empresa diferente"));
    }

    @Test
    void tudoComoEsperadoNaoGeraAlerta() {
        Resultado r = analisar(LUZ_LINHA,
                new DadosEsperados(new BigDecimal("187.52"), null, null, Segmento.ENERGIA_E_GAS, "0048"));

        assertEquals(Classificacao.SEM_ALERTAS, r.classificacao());
    }

    @Test
    void bancoEVencimentoEsperadosSaoIgnoradosNaConta() {
        // conta de consumo não tem banco nem vencimento no código
        Resultado r = analisar(LUZ_LINHA, new DadosEsperados(null, "341", LocalDate.of(2026, 1, 1)));

        assertEquals(Classificacao.SEM_ALERTAS, r.classificacao());
    }

    @Test
    void tipoEsperadoNaoAfetaBoleto() {
        Resultado r = analisar(VALIDO_LINHA, new DadosEsperados(null, null, null, Segmento.ENERGIA_E_GAS, "0048"));
        assertFalse(temAlerta(r, "Tipo de conta"));
    }
}
