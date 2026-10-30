package br.fiap.feedback.receber.repository;

import br.fiap.feedback.domain.Avaliacao;

/** Porta de persistência das avaliações. A implementação real usa DynamoDB. */
public interface AvaliacaoRepository {

    /** Grava uma avaliação nova. */
    void salvar(Avaliacao avaliacao);
}
