package br.com.fiap.techchallenge4.avaliacao.service;

import br.com.fiap.techchallenge4.avaliacao.dto.AvaliacaoRequest;
import br.com.fiap.techchallenge4.shared.exception.ValidacaoException;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;
import br.com.fiap.techchallenge4.avaliacao.publisher.CriticalEventPublisher;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository repository;

    @Mock
    private CriticalEventPublisher publisher;

    private AvaliacaoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-11T18:00:00Z"), ZoneOffset.UTC);
        service = new AvaliacaoService(repository, publisher, clock);
    }

    @Test
    void deveCriarEPublicarAvaliacaoCritica() {
        var avaliacao = service.criar(new AvaliacaoRequest("Aula com problema grave", 2));

        assertEquals(Urgencia.CRITICA, avaliacao.urgencia());
        assertEquals("2026-09-11T18:00Z", avaliacao.dataEnvio());
        verify(repository).save(avaliacao);
        verify(publisher).publish(avaliacao);
    }

    @Test
    void naoDevePublicarEventoQuandoAvaliacaoNaoForCritica() {
        var avaliacao = service.criar(new AvaliacaoRequest("Boa aula", 9));

        assertEquals(Urgencia.BAIXA, avaliacao.urgencia());
        verify(repository).save(avaliacao);
        verify(publisher, never()).publish(avaliacao);
    }

    @Test
    void deveRejeitarNotaForaDoIntervalo() {
        assertThrows(ValidacaoException.class,
                () -> service.criar(new AvaliacaoRequest("Inválida", 11)));
    }
}
