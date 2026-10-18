package br.com.fiap.techchallenge4.avaliacao.publisher;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;

public interface CriticalEventPublisher {
    void publish(Avaliacao avaliacao);
}
