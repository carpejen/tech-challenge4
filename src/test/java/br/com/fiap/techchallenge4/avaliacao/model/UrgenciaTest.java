package br.com.fiap.techchallenge4.avaliacao.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrgenciaTest {

    @Test
    void deveClassificarUrgenciaPelaNota() {
        assertEquals(Urgencia.CRITICA, Urgencia.fromNota(0));
        assertEquals(Urgencia.CRITICA, Urgencia.fromNota(3));
        assertEquals(Urgencia.MEDIA, Urgencia.fromNota(4));
        assertEquals(Urgencia.MEDIA, Urgencia.fromNota(6));
        assertEquals(Urgencia.BAIXA, Urgencia.fromNota(7));
        assertEquals(Urgencia.BAIXA, Urgencia.fromNota(10));
    }

    @Test
    void deveRejeitarNotaForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> Urgencia.fromNota(-1));
        assertThrows(IllegalArgumentException.class, () -> Urgencia.fromNota(11));
    }
}
