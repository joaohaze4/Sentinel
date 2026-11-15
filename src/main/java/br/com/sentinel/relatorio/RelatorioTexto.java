package br.com.sentinel.relatorio;

import br.com.sentinel.analise.Alerta;
import br.com.sentinel.analise.Resultado;
import br.com.sentinel.boleto.Bancos;
import br.com.sentinel.boleto.Boleto;
import br.com.sentinel.boleto.ContaDeConsumo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Monta o texto que aparece no terminal. */
public final class RelatorioTexto {

    private static final String LINHA = "=".repeat(60);
    private static final int LARGURA = 76;

    private RelatorioTexto() {
    }

    public static String gerar(Resultado resultado, LocalDate hoje) {
        StringBuilder sb = new StringBuilder();

        sb.append(LINHA).append('\n');
        sb.append("  SENTINEL - análise antes do pagamento\n");
        sb.append(LINHA).append("\n\n");

        if (resultado.documento() instanceof Boleto b) {
            descreverBoleto(sb, b, hoje);
        } else if (resultado.documento() instanceof ContaDeConsumo c) {
            descreverConta(sb, c);
        }

        sb.append("Resultado       : ").append(resultado.classificacao().name().replace('_', ' '))
                .append("  (pontuação ").append(resultado.pontuacao()).append("/100)\n");
        sb.append("O que fazer     : ")
                .append(quebrar(resultado.classificacao().recomendacao(), 18)).append("\n\n");

        if (resultado.alertas().isEmpty()) {
            sb.append("Alertas         : nenhum\n");
        } else {
            sb.append("Alertas:\n");
            for (Alerta alerta : resultado.alertas()) {
                String etiqueta = String.format("[%s]", alerta.nivel().name().replace("MEDIO", "MÉDIO"));
                sb.append(String.format("  %-8s %s%n", etiqueta, alerta.titulo()));
                sb.append("           ").append(quebrar(alerta.detalhe(), 11)).append('\n');
            }
        }

        sb.append('\n');
        sb.append("Aviso: o Sentinel confere só os números do código. Ele NÃO confirma quem\n");
        sb.append("recebe o dinheiro. Antes de pagar, veja o nome do beneficiário no app do banco.\n");

        return sb.toString();
    }

    private static void descreverBoleto(StringBuilder sb, Boleto b, LocalDate hoje) {
        sb.append("Tipo            : boleto bancário\n");
        sb.append("Linha digitável : ").append(Formatador.linhaDigitavel(b.linhaDigitavel())).append('\n');
        sb.append("Banco           : ").append(Bancos.descricao(b.codigoBanco())).append('\n');
        sb.append("Valor           : ")
                .append(b.semValor() ? "não definido no código" : Formatador.moeda(b.valor())).append('\n');
        sb.append("Vencimento      : ").append(descreverVencimento(b, hoje)).append("\n\n");
    }

    private static void descreverConta(StringBuilder sb, ContaDeConsumo c) {
        sb.append("Tipo            : conta de consumo / tributo\n");
        sb.append("Linha digitável : ").append(Formatador.linhaDigitavelConta(c.linhaDigitavel())).append('\n');
        sb.append("Segmento        : ").append(c.codigoSegmento()).append(" (")
                .append(c.segmento().map(s -> s.descricao()).orElse("não existe")).append(")\n");
        sb.append("Empresa         : ").append(descreverEmpresa(c)).append('\n');
        sb.append("Valor           : ").append(descreverValor(c)).append('\n');
        sb.append("Vencimento      : não vem no código (confira na conta)\n\n");
    }

    static String descreverEmpresa(ContaDeConsumo c) {
        return c.empresaPorCnpj()
                ? "CNPJ (raiz) " + Formatador.cnpjRaiz(c.empresa())
                : "código " + c.empresa();
    }

    static String descreverValor(ContaDeConsumo c) {
        if (c.semValor()) {
            return "não definido no código";
        }
        if (!c.valorEmReais()) {
            return c.valor().toPlainString() + " (moeda de referência, não é em reais)";
        }
        return Formatador.moeda(c.valor());
    }

    /**
     * Quebra um texto longo em várias linhas de no máximo {@code LARGURA} caracteres,
     * alinhando as linhas seguintes com {@code recuo} espaços.
     */
    static String quebrar(String texto, int recuo) {
        StringBuilder resultado = new StringBuilder();
        int tamanhoLinha = recuo;

        for (String palavra : texto.split(" ")) {
            if (tamanhoLinha + palavra.length() > LARGURA && tamanhoLinha > recuo) {
                resultado.append('\n').append(" ".repeat(recuo));
                tamanhoLinha = recuo;
            } else if (tamanhoLinha > recuo) {
                resultado.append(' ');
                tamanhoLinha++;
            }
            resultado.append(palavra);
            tamanhoLinha += palavra.length();
        }
        return resultado.toString();
    }

    private static String descreverVencimento(Boleto b, LocalDate hoje) {
        if (b.semVencimento()) {
            return "sem vencimento";
        }

        long dias = ChronoUnit.DAYS.between(hoje, b.vencimento());
        String quando;

        if (dias == 0) {
            quando = "vence hoje";
        } else if (dias > 0) {
            quando = "daqui a " + dias + " dia(s)";
        } else {
            quando = "venceu há " + (-dias) + " dia(s)";
        }

        return Formatador.data(b.vencimento()) + " (" + quando + ")";
    }
}
