package br.fiap.feedback.receber.function;

/** Corpo do {@code POST /avaliacao}. */
public record AvaliacaoRequest(String descricao, Integer nota) {
}
