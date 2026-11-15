package br.com.sentinel;

import br.com.sentinel.analise.AnalisadorDeRisco;
import br.com.sentinel.analise.Classificacao;
import br.com.sentinel.analise.DadosEsperados;
import br.com.sentinel.analise.Resultado;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.EntradaInvalidaException;
import br.com.sentinel.boleto.LeitorDeBoleto;
import br.com.sentinel.cli.Argumentos;
import br.com.sentinel.relatorio.RelatorioTexto;
import br.com.sentinel.web.ServidorWeb;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Ponto de entrada do programa.
 *
 * <p>Códigos de saída (úteis para usar o Sentinel dentro de outros scripts):</p>
 * <ul>
 *   <li>0 = análise feita, sem alto risco</li>
 *   <li>1 = entrada inválida</li>
 *   <li>2 = análise feita, ALTO RISCO</li>
 * </ul>
 */
public class Main {

    private static final String AJUDA = """
            Sentinel - verifica boletos antes do pagamento

            Uso:
              sentinel CODIGO [opções]
              sentinel                 (sem nada: modo interativo, ele pergunta tudo)
              sentinel --web           abre a versão web em http://localhost:8080

            CODIGO: linha digitável (47 dígitos) ou código de barras (44 dígitos).
                    Pontos e espaços são ignorados.

            Opções (o que você ESPERA pagar, da fatura ou do contrato):
              --valor 250,00           valor esperado
              --banco 341              código do banco esperado
              --vencimento 10/12/2026  vencimento esperado
              --hoje 2026-11-15        muda a data de referência (para testes)
              --porta 8080             porta da versão web (com --web)
              --ajuda                  mostra esta mensagem
            """;

    public static void main(String[] args) {
        PrintStream saida = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        try {
            Argumentos argumentos = Argumentos.ler(args);

            if (argumentos.ajuda()) {
                saida.print(AJUDA);
                return;
            }

            if (argumentos.web()) {
                iniciarWeb(argumentos.porta(), saida);
                return;
            }

            if (argumentos.codigo() == null) {
                argumentos = perguntar(argumentos, saida);
            }

            Boleto boleto = LeitorDeBoleto.ler(argumentos.codigo(), argumentos.hoje());
            Resultado resultado = new AnalisadorDeRisco()
                    .analisar(boleto, argumentos.esperado(), argumentos.hoje());

            saida.print(RelatorioTexto.gerar(resultado, argumentos.hoje()));

            if (resultado.classificacao() == Classificacao.ALTO_RISCO) {
                System.exit(2);
            }
        } catch (EntradaInvalidaException e) {
            saida.println("Erro: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Modo interativo: pergunta o código e, opcionalmente, o que o usuário esperava. */
    private static Argumentos perguntar(Argumentos base, PrintStream saida) {
        Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8);

        saida.print("Cole a linha digitável ou o código de barras: ");
        String codigo = lerLinha(teclado);

        saida.println("\nAgora o que você ESPERA pagar (Enter para pular):");
        saida.print("  Valor (ex.: 250,00): ");
        String valor = lerLinha(teclado);
        saida.print("  Código do banco (ex.: 341): ");
        String banco = lerLinha(teclado);
        saida.print("  Vencimento (ex.: 10/12/2026): ");
        String vencimento = lerLinha(teclado);
        saida.println();

        DadosEsperados esperado = new DadosEsperados(
                valor.isBlank() ? null : Argumentos.lerValor(valor),
                banco.isBlank() ? null : Argumentos.lerBanco(banco),
                vencimento.isBlank() ? null : Argumentos.lerData(vencimento));

        return new Argumentos(codigo, esperado, base.hoje(), false, false, base.porta());
    }

    /** Sobe o servidor web. Ele continua rodando até o usuário apertar Ctrl+C. */
    private static void iniciarWeb(int porta, PrintStream saida) {
        try {
            ServidorWeb servidor = new ServidorWeb(porta);
            servidor.iniciar();
            saida.println("Sentinel rodando na porta " + servidor.porta() + ".");
            saida.println("No seu computador, abra http://localhost:" + servidor.porta()
                    + " no navegador. Para parar, aperte Ctrl+C.");
        } catch (IOException e) {
            saida.println("Erro: não consegui abrir a porta " + porta
                    + ". Ela pode estar em uso; tente outra com --porta 8081.");
            System.exit(1);
        }
    }

    private static String lerLinha(Scanner teclado) {
        return teclado.hasNextLine() ? teclado.nextLine().trim() : "";
    }
}
