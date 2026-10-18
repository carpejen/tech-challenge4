package br.com.fiap.techchallenge4.avaliacao.service;

import br.com.fiap.techchallenge4.avaliacao.dto.AvaliacaoRequest;
import br.com.fiap.techchallenge4.shared.exception.ValidacaoException;
import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;
import br.com.fiap.techchallenge4.avaliacao.publisher.CriticalEventPublisher;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final CriticalEventPublisher criticalEventPublisher;
    private final Clock clock;

    public AvaliacaoService(AvaliacaoRepository repository,
                            CriticalEventPublisher criticalEventPublisher,
                            Clock clock) {
        this.repository = repository;
        this.criticalEventPublisher = criticalEventPublisher;
        this.clock = clock;
    }

    public Avaliacao criar(AvaliacaoRequest request) {
        validar(request);

        Instant agora = clock.instant();
        Urgencia urgencia = Urgencia.fromNota(request.nota());

        Avaliacao avaliacao = new Avaliacao(
                UUID.randomUUID().toString(),
                request.descricao().trim(),
                request.nota(),
                urgencia,
                OffsetDateTime.ofInstant(agora, ZoneOffset.UTC).toString(),
                agora.getEpochSecond()
        );

        repository.save(avaliacao);

        if (urgencia == Urgencia.CRITICA) {
            criticalEventPublisher.publish(avaliacao);
        }

        return avaliacao;
    }

    private void validar(AvaliacaoRequest request) {
        if (request == null) {
            throw new ValidacaoException("O corpo da requisição é obrigatório.");
        }
        if (request.descricao() == null || request.descricao().isBlank()) {
            throw new ValidacaoException("A descrição é obrigatória.");
        }
        if (request.nota() == null || request.nota() < 0 || request.nota() > 10) {
            throw new ValidacaoException("A nota deve estar entre 0 e 10.");
        }
    }
}
