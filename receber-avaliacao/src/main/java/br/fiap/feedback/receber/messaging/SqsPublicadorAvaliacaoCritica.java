package br.fiap.feedback.receber.messaging;

import br.fiap.feedback.contrato.MensagemAvaliacaoCritica;
import br.fiap.feedback.domain.Avaliacao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Publica avaliações críticas na fila SQS (com DLQ configurada no template SAM). */
@Singleton
public class SqsPublicadorAvaliacaoCritica implements PublicadorAvaliacaoCritica {

    private final SqsClient sqs;
    private final ObjectMapper json;
    private final String filaUrl;

    @Inject
    public SqsPublicadorAvaliacaoCritica(SqsClient sqs,
                                         ObjectMapper json,
                                         @ConfigProperty(name = "app.fila-criticas-url") String filaUrl) {
        this.sqs = sqs;
        this.json = json;
        this.filaUrl = filaUrl;
    }

    @Override
    public void publicar(Avaliacao avaliacao) {
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(filaUrl)
                .messageBody(paraJson(avaliacao))
                .build());
    }

    String paraJson(Avaliacao avaliacao) {
        try {
            return json.writeValueAsString(MensagemAvaliacaoCritica.de(avaliacao));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar a avaliação " + avaliacao.id(), e);
        }
    }
}
