package br.com.sentinel.boleto;

/** Lançada quando o texto informado nem chega a ser um boleto (tamanho errado, letras...). */
public class EntradaInvalidaException extends IllegalArgumentException {

    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
