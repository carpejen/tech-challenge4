package br.fiap.feedback.contrato;

import br.fiap.feedback.domain.Avaliacao;

/**
 * Contrato da mensagem na fila {@code avaliacoes-criticas}.
 *
 * <p>Produzida por {@code receber-avaliacao} e consumida por {@code notificar-urgencia}.
 * Traz os dados que o e-mail de urgência exige: descrição, urgência e data de envio.
 * Fica no módulo {@code shared} para os dois lados usarem exatamente o mesmo formato.
 */
public record MensagemAvaliacaoCritica(String id, String descricao, int nota, String urgencia, String dataEnvio) {

    public static MensagemAvaliacaoCritica de(Avaliacao avaliacao) {
        return new MensagemAvaliacaoCritica(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia().name(),
                avaliacao.dataEnvio().toString());
    }
}
