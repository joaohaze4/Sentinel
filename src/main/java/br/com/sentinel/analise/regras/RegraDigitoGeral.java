package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.DigitoVerificador;

import java.time.LocalDate;
import java.util.List;

/**
 * Confere o dígito verificador geral (posição 5 do código de barras, módulo 11).
 * Ele "protege" o código inteiro: banco, valor, vencimento e campo livre.
 */
public class RegraDigitoGeral implements Regra<Boleto> {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        String barras = boleto.codigoDeBarras();
        String semDigito = barras.substring(0, 4) + barras.substring(5);

        int calculado = DigitoVerificador.modulo11(semDigito);

        if (calculado == boleto.digitoGeral()) {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.ALTO,
                "Dígito verificador geral não confere",
                "O código informa " + boleto.digitoGeral() + ", mas a conta dá " + calculado
                        + ". Algum número do boleto foi digitado errado ou alterado."));
    }
}
