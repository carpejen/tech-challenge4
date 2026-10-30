package br.fiap.feedback.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvaliacaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T14:22:10Z");

    @Test
    void criaAvaliacaoValidaComUrgenciaCalculada() {
        Avaliacao avaliacao = Avaliacao.nova("id-1", "  Áudio da aula ruim  ", 2, AGORA);

        assertEquals("id-1", avaliacao.id());
        assertEquals("Áudio da aula ruim", avaliacao.descricao());
        assertEquals(2, avaliacao.nota());
        assertEquals(Urgencia.CRITICA, avaliacao.urgencia());
        assertEquals(AGORA, avaliacao.dataEnvio());
    }

    @Test
    void descricaoEhObrigatoria() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("id", null, 5, AGORA));
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("id", "   ", 5, AGORA));
    }

    @Test
    void descricaoTemLimiteDeTamanho() {
        String noLimite = "a".repeat(Avaliacao.TAMANHO_MAXIMO_DESCRICAO);
        assertEquals(noLimite, Avaliacao.nova("id", noLimite, 5, AGORA).descricao());

        String acimaDoLimite = noLimite + "a";
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("id", acimaDoLimite, 5, AGORA));
    }

    @Test
    void notaEhObrigatoriaEValidada() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("id", "ok", null, AGORA));
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("id", "ok", 11, AGORA));
    }

    @Test
    void diaUsaHorarioDeBrasilia() {
        // 01:30 UTC do dia 4 ainda é 22:30 do dia 3 em São Paulo
        Avaliacao avaliacao = Avaliacao.nova("id", "ok", 9, Instant.parse("2026-11-04T01:30:00Z"));
        assertEquals(LocalDate.of(2026, 11, 3), avaliacao.dia());
    }
}
