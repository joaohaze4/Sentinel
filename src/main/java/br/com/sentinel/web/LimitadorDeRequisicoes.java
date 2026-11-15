package br.com.sentinel.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Limita quantas requisições cada cliente (IP) pode fazer por minuto.
 *
 * <p>Sem isso, alguém poderia mandar milhares de requisições e derrubar o site
 * (ataque de negação de serviço, ou DoS).</p>
 *
 * <p>Técnica usada: <b>janela fixa</b>. A cada minuto do relógio a contagem zera.
 * É a forma mais simples; a desvantagem é permitir uma rajada na virada do minuto
 * (até 2x o limite em poucos segundos), o que aqui não é problema.</p>
 *
 * <p>Há dois limites:</p>
 * <ul>
 *   <li><b>por cliente</b>: impede que uma pessoa sozinha abuse;</li>
 *   <li><b>global</b>: protege mesmo se o atacante fingir ser vários IPs diferentes.
 *       Também garante que o mapa de clientes nunca cresça sem parar (o que
 *       poderia estourar a memória).</li>
 * </ul>
 */
public final class LimitadorDeRequisicoes {

    private final int limitePorCliente;
    private final int limiteGlobal;
    private final LongSupplier relogioEmMilissegundos;

    private long minutoAtual = -1;
    private int totalNoMinuto = 0;
    private final Map<String, Integer> usadosPorCliente = new HashMap<>();

    public LimitadorDeRequisicoes(int limitePorCliente, int limiteGlobal) {
        this(limitePorCliente, limiteGlobal, System::currentTimeMillis);
    }

    /** @param relogioEmMilissegundos nos testes, um relógio "de mentira" que a gente controla */
    LimitadorDeRequisicoes(int limitePorCliente, int limiteGlobal, LongSupplier relogioEmMilissegundos) {
        this.limitePorCliente = limitePorCliente;
        this.limiteGlobal = limiteGlobal;
        this.relogioEmMilissegundos = relogioEmMilissegundos;
    }

    /**
     * Registra uma requisição do cliente e diz se ela pode passar.
     *
     * <p>{@code synchronized}: várias requisições chegam ao mesmo tempo (em threads diferentes),
     * e só uma pode mexer na contagem por vez. Senão duas poderiam ler "29" juntas e ambas passarem.</p>
     */
    public synchronized boolean permitir(String cliente) {
        long minuto = relogioEmMilissegundos.getAsLong() / 60_000;

        if (minuto != minutoAtual) {        // virou o minuto: zera tudo
            minutoAtual = minuto;
            totalNoMinuto = 0;
            usadosPorCliente.clear();
        }

        if (totalNoMinuto >= limiteGlobal) {
            return false;                   // checado ANTES de guardar o cliente no mapa
        }

        int usados = usadosPorCliente.getOrDefault(cliente, 0);

        if (usados >= limitePorCliente) {
            return false;
        }

        usadosPorCliente.put(cliente, usados + 1);
        totalNoMinuto++;
        return true;
    }

    /** Quantos segundos faltam para a contagem zerar (vai no cabeçalho Retry-After). */
    public long segundosParaLiberar() {
        long segundoDoMinuto = (relogioEmMilissegundos.getAsLong() / 1000) % 60;
        return 60 - segundoDoMinuto;
    }
}
