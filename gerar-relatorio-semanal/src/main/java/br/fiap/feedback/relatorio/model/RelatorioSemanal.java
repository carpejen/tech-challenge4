package br.fiap.feedback.relatorio.model;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consolidação das avaliações de sete dias usada no e-mail semanal. */
public record RelatorioSemanal(
        LocalDate inicio,
        LocalDate fim,
        int totalAvaliacoes,
        Double mediaNotas,
        Map<LocalDate, Long> quantidadePorDia,
        Map<Urgencia, Long> quantidadePorUrgencia,
        List<Avaliacao> avaliacoes) {

    public RelatorioSemanal {
        quantidadePorDia = Collections.unmodifiableMap(new LinkedHashMap<>(quantidadePorDia));
        EnumMap<Urgencia, Long> copiaUrgencias = new EnumMap<>(Urgencia.class);
        copiaUrgencias.putAll(quantidadePorUrgencia);
        quantidadePorUrgencia = Collections.unmodifiableMap(copiaUrgencias);
        avaliacoes = List.copyOf(avaliacoes);
    }

    public static RelatorioSemanal consolidar(LocalDate inicio, LocalDate fim, List<Avaliacao> recebidas) {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim do período não pode ser anterior ao início");
        }

        List<Avaliacao> avaliacoes = new ArrayList<>(recebidas);
        avaliacoes.sort(Comparator.comparing(Avaliacao::dataEnvio));

        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            porDia.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }

        Map<Urgencia, Long> porUrgencia = new EnumMap<>(Urgencia.class);
        for (Urgencia urgencia : Urgencia.values()) {
            porUrgencia.put(urgencia, 0L);
        }

        long somaNotas = 0;
        for (Avaliacao avaliacao : avaliacoes) {
            porDia.computeIfPresent(avaliacao.dia(), (dia, quantidade) -> quantidade + 1);
            porUrgencia.compute(avaliacao.urgencia(), (urgencia, quantidade) -> quantidade == null ? 1L : quantidade + 1);
            somaNotas += avaliacao.nota();
        }

        Double media = avaliacoes.isEmpty() ? null : (double) somaNotas / avaliacoes.size();

        return new RelatorioSemanal(
                inicio,
                fim,
                avaliacoes.size(),
                media,
                porDia,
                porUrgencia,
                avaliacoes);
    }
}
