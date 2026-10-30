package br.fiap.feedback.receber.messaging;

import br.fiap.feedback.domain.Avaliacao;

/**
 * Porta de saída para avaliações críticas. A implementação real publica numa fila SQS,
 * consumida pela função {@code notificar-urgencia}.
 */
public interface PublicadorAvaliacaoCritica {

    /** Publica a avaliação para notificação dos administradores. */
    void publicar(Avaliacao avaliacao);
}
