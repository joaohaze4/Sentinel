package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Bancos;
import br.com.sentinel.boleto.Boleto;

import java.time.LocalDate;
import java.util.List;

/** Avisa quando o código do banco não está na lista conhecida. */
public class RegraBancoConhecido implements Regra {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        if (Bancos.nome(boleto.codigoBanco()).isPresent()) {
            return List.of();
        }

        return List.of(new Alerta(
                Nivel.MEDIO,
                "Banco " + boleto.codigoBanco() + " não reconhecido",
                "Pode ser um banco pequeno ou cooperativa fora da lista do Sentinel, "
                        + "mas também pode ser um código inventado. Confira o banco impresso no boleto."));
    }
}
