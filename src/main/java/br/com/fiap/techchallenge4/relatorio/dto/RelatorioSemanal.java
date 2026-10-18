package br.com.fiap.techchallenge4.relatorio.dto;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;

import java.util.List;
import java.util.Map;

public record RelatorioSemanal(
        String periodoInicio,
        String periodoFim,
        long totalAvaliacoes,
        double mediaAvaliacoes,
        Map<String, Long> quantidadePorDia,
        Map<Urgencia, Long> quantidadePorUrgencia,
        List<Avaliacao> avaliacoes
) {
}
