package br.com.sentinel.analise.regras.conta;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.ContaDeConsumo;
import br.com.sentinel.boleto.DigitoVerificador;
import br.com.sentinel.boleto.Origem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Confere o dígito verificador de cada um dos 4 blocos da linha digitável da conta.
 * O módulo (10 ou 11) depende da 3ª posição do código.
 */
public class RegraContaDigitosDosBlocos implements Regra<ContaDeConsumo> {

    @Override
    public List<Alerta> avaliar(ContaDeConsumo conta, DadosEsperados esperado, LocalDate hoje) {
        // se a 3ª posição é inválida, nem dá para saber qual módulo usar (outra regra já avisa)
        if (conta.origem() != Origem.LINHA_DIGITAVEL || !conta.identificacaoValida()) {
            return List.of();
        }

        String linha = conta.linhaDigitavel();
        List<Alerta> alertas = new ArrayList<>();

        for (int bloco = 0; bloco < 4; bloco++) {
            int inicio = bloco * 12;
            String numeros = linha.substring(inicio, inicio + 11);
            int informado = linha.charAt(inicio + 11) - '0';
            int calculado = DigitoVerificador.arrecadacao(conta.identificacaoValor(), numeros);

            if (informado != calculado) {
                alertas.add(new Alerta(
                        Nivel.ALTO,
                        "Dígito do bloco " + (bloco + 1) + " não confere",
                        "O bloco " + (bloco + 1) + " termina em " + informado + ", mas a conta dá " + calculado
                                + ". Pode ser erro de digitação ou conta adulterada. "
                                + "Digite de novo com calma; se continuar, não pague."));
            }
        }
        return alertas;
    }
}
