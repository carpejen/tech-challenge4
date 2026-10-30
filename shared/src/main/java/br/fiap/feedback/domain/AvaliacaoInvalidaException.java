package br.fiap.feedback.domain;

/**
 * Lançada quando os dados de uma avaliação violam alguma regra de negócio
 * (descrição vazia, nota fora de 0 a 10 etc.). Vira HTTP 400 na borda.
 */
public class AvaliacaoInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AvaliacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
