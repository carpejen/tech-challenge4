package br.com.fiap.techchallenge4.avaliacao.model;

public record Avaliacao(
        String id,
        String descricao,
        int nota,
        Urgencia urgencia,
        String dataEnvio,
        long timestampEpoch
) {
}
