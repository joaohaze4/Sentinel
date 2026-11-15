package br.com.sentinel.analise;

import br.com.sentinel.boleto.LeitorDeBoleto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.sentinel.Exemplos.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalisadorDeRiscoTest {

    private final AnalisadorDeRisco analisador = new AnalisadorDeRisco();

    private Resultado analisar(String codigo) {
        return analisar(codigo, DadosEsperados.nenhum());
    }

    private Resultado analisar(String codigo, DadosEsperados esperado) {
        return analisador.analisar(LeitorDeBoleto.ler(codigo, HOJE), esperado, HOJE);
    }

    private boolean temAlerta(Resultado r, String trechoDoTitulo) {
        return r.alertas().stream().anyMatch(a -> a.titulo().contains(trechoDoTitulo));
    }

    @Test
    void boletoValidoNaoTemAlertas() {
        Resultado r = analisar(VALIDO_LINHA);

        assertEquals(Classificacao.SEM_ALERTAS, r.classificacao());
        assertEquals(0, r.pontuacao());
        assertTrue(r.alertas().isEmpty());
    }

    @Test
    void codigoDeBarrasValidoNaoTemAlertas() {
        assertEquals(Classificacao.SEM_ALERTAS, analisar(VALIDO_BARRAS).classificacao());
    }

    @Test
    void linhaAdulteradaEAltoRisco() {
        Resultado r = analisar(ADULTERADO_LINHA);

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "campo 2"));
        assertTrue(temAlerta(r, "verificador geral"));
    }

    @Test
    void golpeBemFeitoPassaNaMatematicaMasNaoNoBancoEsperado() {
        // sem dados esperados, o boleto falso parece perfeito...
        assertEquals(Classificacao.SEM_ALERTAS, analisar(BANCO_TROCADO_LINHA).classificacao());

        // ...mas informando o banco que o fornecedor sempre usa, o golpe aparece
        Resultado r = analisar(BANCO_TROCADO_LINHA, new DadosEsperados(null, "341", null));
        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Banco diferente"));
    }

    @Test
    void valorDiferenteDoEsperadoEAltoRisco() {
        Resultado r = analisar(VALIDO_LINHA, new DadosEsperados(new BigDecimal("199.90"), null, null));

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Valor diferente"));
    }

    @Test
    void valorIgualComEscalaDiferenteNaoGeraAlerta() {
        // 250.0 e 250.00 são o mesmo valor
        Resultado r = analisar(VALIDO_LINHA, new DadosEsperados(new BigDecimal("250.0"), "341", null));
        assertEquals(Classificacao.SEM_ALERTAS, r.classificacao());
    }

    @Test
    void vencimentoDiferenteDoEsperadoPedeAtencao() {
        Resultado r = analisar(VALIDO_LINHA, new DadosEsperados(null, null, LocalDate.of(2026, 12, 5)));

        assertEquals(Classificacao.ATENCAO, r.classificacao());
        assertTrue(temAlerta(r, "Vencimento diferente"));
    }

    @Test
    void boletoVencidoPedeAtencao() {
        Resultado r = analisar(VENCIDO_LINHA);

        assertEquals(Classificacao.ATENCAO, r.classificacao());
        assertTrue(temAlerta(r, "vencido há 45 dia(s)"));
    }

    @Test
    void vencimentoMuitoDistantePedeAtencao() {
        assertTrue(temAlerta(analisar(VENCIMENTO_LONGE_LINHA), "muito distante"));
    }

    @Test
    void semVencimentoNaoEProblema() {
        assertEquals(Classificacao.SEM_ALERTAS, analisar(SEM_VENCIMENTO_LINHA).classificacao());
    }

    @Test
    void bancoDesconhecidoPedeAtencao() {
        Resultado r = analisar(BANCO_DESCONHECIDO_LINHA);

        assertEquals(Classificacao.ATENCAO, r.classificacao());
        assertTrue(temAlerta(r, "999 não reconhecido"));
    }

    @Test
    void semValorEBaixoRisco() {
        Resultado r = analisar(SEM_VALOR_LINHA);

        assertEquals(Classificacao.BAIXO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "Sem valor"));
    }

    @Test
    void semValorNaoComparaComValorEsperado() {
        Resultado r = analisar(SEM_VALOR_LINHA, new DadosEsperados(new BigDecimal("100.00"), null, null));
        assertTrue(!temAlerta(r, "Valor diferente"));
    }

    @Test
    void moedaErradaEAltoRisco() {
        Resultado r = analisar(MOEDA_ERRADA_LINHA);

        assertEquals(Classificacao.ALTO_RISCO, r.classificacao());
        assertTrue(temAlerta(r, "moeda"));
    }

    @Test
    void alertasMaisGravesVemPrimeiroEPontuacaoTemTeto() {
        Resultado r = analisar(ADULTERADO_LINHA,
                new DadosEsperados(new BigDecimal("10.00"), "001", LocalDate.of(2026, 1, 1)));

        assertEquals(Nivel.ALTO, r.alertas().get(0).nivel());
        assertEquals(100, r.pontuacao());
    }
}
