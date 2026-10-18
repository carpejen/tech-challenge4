package br.com.fiap.techchallenge4.avaliacao.publisher;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class SnsCriticalEventPublisher implements CriticalEventPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public SnsCriticalEventPublisher(SnsClient snsClient, ObjectMapper objectMapper, String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    @Override
    public void publish(Avaliacao avaliacao) {
        try {
            String payload = objectMapper.writeValueAsString(avaliacao);
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(payload)
                    .subject("Feedback crítico recebido")
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar a avaliação crítica.", e);
        }
    }
}
