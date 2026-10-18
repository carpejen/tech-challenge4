package br.com.fiap.techchallenge4.avaliacao.dto;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;

public record AvaliacaoResponse(
        String id,
        String descricao,
        int nota,
        Urgencia urgencia,
        String dataEnvio
) {
    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia(),
                avaliacao.dataEnvio()
        );
    }
}
