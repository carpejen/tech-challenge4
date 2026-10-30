package br.fiap.feedback.receber.service;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.AvaliacaoInvalidaException;
import br.fiap.feedback.domain.Urgencia;
import br.fiap.feedback.receber.messaging.PublicadorAvaliacaoCritica;
import br.fiap.feedback.receber.repository.AvaliacaoRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvaliacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T14:22:10Z");

    private final RepositorioEmMemoria repositorio = new RepositorioEmMemoria();
    private final PublicadorEmMemoria publicador = new PublicadorEmMemoria();
    private final AvaliacaoService service =
            new AvaliacaoService(repositorio, publicador, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void gravaAvaliacaoNaoCriticaSemPublicarNaFila() {
        Avaliacao avaliacao = service.registrar("Aula ótima", 9);

        assertNotNull(avaliacao.id());
        assertEquals(Urgencia.BAIXA, avaliacao.urgencia());
        assertEquals(AGORA, avaliacao.dataEnvio());
        assertEquals(List.of(avaliacao), repositorio.salvas);
        assertTrue(publicador.publicadas.isEmpty());
    }

    @Test
    void gravaEPublicaAvaliacaoCritica() {
        Avaliacao avaliacao = service.registrar("Áudio ruim", 2);

        assertEquals(Urgencia.CRITICA, avaliacao.urgencia());
        assertEquals(List.of(avaliacao), repositorio.salvas);
        assertEquals(List.of(avaliacao), publicador.publicadas);
    }

    @Test
    void falhaNaFilaNaoDesfazNemImpedeORegistro() {
        publicador.falhar = true;

        Avaliacao avaliacao = service.registrar("Áudio ruim", 0);

        assertEquals(List.of(avaliacao), repositorio.salvas);
    }

    @Test
    void avaliacaoInvalidaNaoEhGravada() {
        assertThrows(AvaliacaoInvalidaException.class, () -> service.registrar("", 5));
        assertThrows(AvaliacaoInvalidaException.class, () -> service.registrar("ok", 42));

        assertTrue(repositorio.salvas.isEmpty());
        assertTrue(publicador.publicadas.isEmpty());
    }

    @Test
    void cadaAvaliacaoRecebeUmIdDiferente() {
        String primeiro = service.registrar("a", 8).id();
        String segundo = service.registrar("b", 8).id();

        assertTrue(!primeiro.equals(segundo));
    }

    static class RepositorioEmMemoria implements AvaliacaoRepository {
        final List<Avaliacao> salvas = new ArrayList<>();

        @Override
        public void salvar(Avaliacao avaliacao) {
            salvas.add(avaliacao);
        }
    }

    static class PublicadorEmMemoria implements PublicadorAvaliacaoCritica {
        final List<Avaliacao> publicadas = new ArrayList<>();
        boolean falhar;

        @Override
        public void publicar(Avaliacao avaliacao) {
            if (falhar) {
                throw new IllegalStateException("fila indisponível");
            }
            publicadas.add(avaliacao);
        }
    }
}
