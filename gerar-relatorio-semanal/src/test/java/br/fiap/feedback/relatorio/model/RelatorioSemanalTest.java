package br.fiap.feedback.relatorio.model;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RelatorioSemanalTest {

    @Test
    void consolidaMediaQuantidadePorDiaEPorUrgencia() {
        RelatorioSemanal relatorio = RelatorioSemanal.consolidar(
                LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 11, 1),
                List.of(
                        avaliacao("1", 2, Urgencia.CRITICA, "2026-10-26T12:00:00Z"),
                        avaliacao("2", 6, Urgencia.MEDIA, "2026-10-26T15:00:00Z"),
                        avaliacao("3", 10, Urgencia.BAIXA, "2026-10-28T12:00:00Z")));

        assertEquals(3, relatorio.totalAvaliacoes());
        assertEquals(6.0, relatorio.mediaNotas(), 0.0001);
        assertEquals(2L, relatorio.quantidadePorDia().get(LocalDate.of(2026, 10, 26)));
        assertEquals(1L, relatorio.quantidadePorDia().get(LocalDate.of(2026, 10, 28)));
        assertEquals(0L, relatorio.quantidadePorDia().get(LocalDate.of(2026, 11, 1)));
        assertEquals(1L, relatorio.quantidadePorUrgencia().get(Urgencia.CRITICA));
        assertEquals(1L, relatorio.quantidadePorUrgencia().get(Urgencia.MEDIA));
        assertEquals(1L, relatorio.quantidadePorUrgencia().get(Urgencia.BAIXA));
    }

    @Test
    void periodoSemAvaliacoesTemMediaNulaEZeros() {
        RelatorioSemanal relatorio = RelatorioSemanal.consolidar(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1), List.of());

        assertEquals(0, relatorio.totalAvaliacoes());
        assertNull(relatorio.mediaNotas());
        assertEquals(7, relatorio.quantidadePorDia().size());
        assertEquals(0L, relatorio.quantidadePorUrgencia().get(Urgencia.CRITICA));
    }

    private static Avaliacao avaliacao(String id, int nota, Urgencia urgencia, String data) {
        return new Avaliacao(id, "Descrição " + id, nota, urgencia, Instant.parse(data));
    }
}
