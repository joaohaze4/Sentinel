package br.com.sentinel.analise.regras;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Nivel;
import br.com.sentinel.analise.Regra;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.DigitoVerificador;
import br.com.sentinel.boleto.Origem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Confere os dígitos verificadores dos campos 1, 2 e 3 da linha digitável (módulo 10).
 *
 * <p>Só faz sentido quando o usuário digitou a linha digitável. Se ele informou o código
 * de barras, a linha é gerada pelo próprio Sentinel e os dígitos sempre vão bater.</p>
 */
public class RegraDigitosDosCampos implements Regra {

    @Override
    public List<Alerta> avaliar(Boleto boleto, DadosEsperados esperado, LocalDate hoje) {
        if (boleto.origem() != Origem.LINHA_DIGITAVEL) {
            return List.of();
        }

        String linha = boleto.linhaDigitavel();
        List<Alerta> alertas = new ArrayList<>();

        conferir(1, linha.substring(0, 9), linha.charAt(9), alertas);
        conferir(2, linha.substring(10, 20), linha.charAt(20), alertas);
        conferir(3, linha.substring(21, 31), linha.charAt(31), alertas);

        return alertas;
    }

    private void conferir(int campo, String numeros, char digitoInformado, List<Alerta> alertas) {
        int esperado = DigitoVerificador.modulo10(numeros);
        int informado = digitoInformado - '0';

        if (esperado != informado) {
            alertas.add(new Alerta(
                    Nivel.ALTO,
                    "Dígito do campo " + campo + " não confere",
                    "O campo " + campo + " termina em " + informado + ", mas a conta dá " + esperado
                            + ". Pode ser erro de digitação ou linha adulterada. "
                            + "Digite de novo com calma; se continuar, não pague."));
        }
    }
}
