package br.fiap.feedback.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgenciaTest {

    @Test
    void notasDeZeroAQuatroSaoCriticas() {
        for (int nota = 0; nota <= 4; nota++) {
            assertEquals(Urgencia.CRITICA, Urgencia.daNota(nota), "nota " + nota);
        }
    }

    @Test
    void notasDeCincoASeteSaoMedias() {
        for (int nota = 5; nota <= 7; nota++) {
            assertEquals(Urgencia.MEDIA, Urgencia.daNota(nota), "nota " + nota);
        }
    }

    @Test
    void notasDeOitoADezSaoBaixas() {
        for (int nota = 8; nota <= 10; nota++) {
            assertEquals(Urgencia.BAIXA, Urgencia.daNota(nota), "nota " + nota);
        }
    }

    @Test
    void notaForaDoIntervaloEhRejeitada() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Urgencia.daNota(-1));
        assertThrows(AvaliacaoInvalidaException.class, () -> Urgencia.daNota(11));
    }

    @Test
    void somenteCriticaExigeNotificacaoImediata() {
        assertTrue(Urgencia.CRITICA.exigeNotificacaoImediata());
        assertFalse(Urgencia.MEDIA.exigeNotificacaoImediata());
        assertFalse(Urgencia.BAIXA.exigeNotificacaoImediata());
    }
}
