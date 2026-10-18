package br.com.fiap.techchallenge4.relatorio.service;

import br.com.fiap.techchallenge4.relatorio.dto.RelatorioSemanal;
import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelatorioService {

    private final AvaliacaoRepository repository;

    public RelatorioService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public RelatorioSemanal gerar(Instant fimPeriodo) {
        Instant inicioPeriodo = fimPeriodo.minus(7, ChronoUnit.DAYS);
        List<Avaliacao> avaliacoes = repository.findSince(inicioPeriodo);

        double media = avaliacoes.stream()
                .mapToInt(Avaliacao::nota)
                .average()
                .orElse(0.0);

        Map<String, Long> porDia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        avaliacao -> OffsetDateTime.parse(avaliacao.dataEnvio()).toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<Urgencia, Long> porUrgencia = new EnumMap<>(Urgencia.class);
        for (Urgencia urgencia : Urgencia.values()) {
            porUrgencia.put(urgencia, 0L);
        }
        avaliacoes.forEach(a -> porUrgencia.compute(a.urgencia(), (k, v) -> v + 1));

        return new RelatorioSemanal(
                OffsetDateTime.ofInstant(inicioPeriodo, ZoneOffset.UTC).toString(),
                OffsetDateTime.ofInstant(fimPeriodo, ZoneOffset.UTC).toString(),
                avaliacoes.size(),
                media,
                porDia,
                porUrgencia,
                avaliacoes
        );
    }

    public String formatar(RelatorioSemanal relatorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relatório semanal de feedbacks\n\n")
                .append("Período: ").append(relatorio.periodoInicio())
                .append(" até ").append(relatorio.periodoFim()).append("\n")
                .append("Total de avaliações: ").append(relatorio.totalAvaliacoes()).append("\n")
                .append("Média das avaliações: ")
                .append(String.format(java.util.Locale.ROOT, "%.2f", relatorio.mediaAvaliacoes()))
                .append("\n\n")
                .append("Quantidade por dia:\n");

        relatorio.quantidadePorDia().forEach((dia, qtd) ->
                sb.append("- ").append(dia).append(": ").append(qtd).append("\n"));

        sb.append("\nQuantidade por urgência:\n");
        relatorio.quantidadePorUrgencia().forEach((urgencia, qtd) ->
                sb.append("- ").append(urgencia).append(": ").append(qtd).append("\n"));

        sb.append("\nAvaliações:\n");
        relatorio.avaliacoes().forEach(a -> sb
                .append("- ").append(a.dataEnvio())
                .append(" | ").append(a.urgencia())
                .append(" | nota ").append(a.nota())
                .append(" | ").append(a.descricao())
                .append("\n"));

        return sb.toString();
    }
}
