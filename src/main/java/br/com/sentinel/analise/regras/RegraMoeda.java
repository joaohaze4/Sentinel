package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Boleto;

import java.time.LocalDate;
import java.util.List;

/** Boleto em real sempre tem o código de moeda 9 (4ª posição). */
public class RegraMoeda implements Regra {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        if (boleto.moeda() == '9') {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.ALTO,
                "Código de moeda inválido",
                "Boletos em real usam o código 9 na 4ª posição, mas aqui está " + boleto.moeda() + "."));
    }
}
