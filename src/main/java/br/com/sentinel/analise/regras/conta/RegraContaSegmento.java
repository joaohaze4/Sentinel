package br.com.sentinel.analise.regras.conta;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.ContaDeConsumo;
import br.com.sentinel.boleto.Segmento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Confere o segmento (2ª posição): 0 e 8 não existem; 9 é reservado aos bancos. */
public class RegraContaSegmento implements Regra<ContaDeConsumo> {

    @Override
    public List<Alerta> avaliar(ContaDeConsumo conta, DadosEsperados esperado, LocalDate hoje) {
        Optional<Segmento> segmento = conta.segmento();

        if (segmento.isEmpty()) {
            return List.of(new Alerta(
                    Nivel.ALTO,
                    "Segmento " + conta.codigoSegmento() + " não existe",
                    "A 2ª posição diz o tipo de conta (3 = energia, 2 = água...). "
                            + "O código " + conta.codigoSegmento() + " não é usado pelos bancos."));
        }

        if (segmento.get() == Segmento.USO_DO_BANCO) {
            return List.of(new Alerta(
                    Nivel.MEDIO,
                    "Segmento de uso exclusivo do banco",
                    "Esse código é reservado para uso interno dos bancos e não deveria aparecer "
                            + "numa conta enviada para você. Confira com quem cobrou."));
        }

        return List.of();
    }
}
