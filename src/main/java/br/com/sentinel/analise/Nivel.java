package br.com.sentinel.analise;

/** Gravidade de um alerta. Cada nível soma pontos na pontuação de risco (0 a 100). */
public enum Nivel {
    BAIXO(10),
    MEDIO(25),
    ALTO(60);

    private final int pontos;

    Nivel(int pontos) {
        this.pontos = pontos;
    }

    public int pontos() {
        return pontos;
    }
}
