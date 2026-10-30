package br.fiap.feedback.receber.function;

import br.fiap.feedback.domain.Avaliacao;

/** Resposta do {@code POST /avaliacao} (HTTP 201). */
public record AvaliacaoResponse(String id, String descricao, int nota, String urgencia, String dataEnvio) {

    static AvaliacaoResponse de(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia().name(),
                avaliacao.dataEnvio().toString());
    }
}
