package br.com.fiap.techchallenge4.notificacao.handler;

import br.com.fiap.techchallenge4.config.AwsClientFactory;
import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.notificacao.service.NotificacaoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificarAvaliacaoCriticaHandler implements RequestHandler<SNSEvent, Void> {

    private final ObjectMapper objectMapper;
    private final NotificacaoService notificacaoService;

    public NotificarAvaliacaoCriticaHandler() {
        this.objectMapper = new ObjectMapper();
        this.notificacaoService = new NotificacaoService(
                AwsClientFactory.snsClient(),
                requiredEnv("ADMIN_TOPIC_ARN")
        );
    }

    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        event.getRecords().forEach(record -> {
            try {
                Avaliacao avaliacao = objectMapper.readValue(record.getSNS().getMessage(), Avaliacao.class);
                notificacaoService.notificarCritico(avaliacao);
            } catch (Exception e) {
                context.getLogger().log("Erro ao notificar avaliação crítica: " + e.getMessage());
                throw new IllegalStateException("Falha ao processar notificação crítica.", e);
            }
        });
        return null;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não configurada: " + name);
        }
        return value;
    }
}
