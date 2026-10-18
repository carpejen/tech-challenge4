package br.com.fiap.techchallenge4.avaliacao.handler;

import br.com.fiap.techchallenge4.config.AwsClientFactory;
import br.com.fiap.techchallenge4.avaliacao.dto.AvaliacaoRequest;
import br.com.fiap.techchallenge4.avaliacao.dto.AvaliacaoResponse;
import br.com.fiap.techchallenge4.shared.dto.ErroResponse;
import br.com.fiap.techchallenge4.shared.exception.ValidacaoException;
import br.com.fiap.techchallenge4.avaliacao.publisher.SnsCriticalEventPublisher;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;
import br.com.fiap.techchallenge4.avaliacao.service.AvaliacaoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Map;

public class ReceberAvaliacaoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");

    private final ObjectMapper objectMapper;
    private final AvaliacaoService service;

    public ReceberAvaliacaoHandler() {
        this.objectMapper = new ObjectMapper();

        String tableName = requiredEnv("TABLE_NAME");
        String criticalTopicArn = requiredEnv("CRITICAL_TOPIC_ARN");

        var repository = new AvaliacaoRepository(AwsClientFactory.dynamoDbClient(), tableName);
        var publisher = new SnsCriticalEventPublisher(AwsClientFactory.snsClient(), objectMapper, criticalTopicArn);
        this.service = new AvaliacaoService(repository, publisher, Clock.systemUTC());
    }

    ReceberAvaliacaoHandler(ObjectMapper objectMapper, AvaliacaoService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            validarCorpo(event);

            AvaliacaoRequest request = deserializar(event.getBody());
            AvaliacaoResponse response = AvaliacaoResponse.from(service.criar(request));

            return response(201, objectMapper.writeValueAsString(response));
        } catch (ValidacaoException e) {
            return error(400, e.getMessage());
        } catch (Exception e) {
            logErro(context, e);
            return error(500, "Erro interno ao processar a avaliação.");
        }
    }

    private void validarCorpo(APIGatewayProxyRequestEvent event) {
        if (event == null || event.getBody() == null || event.getBody().isBlank()) {
            throw new ValidacaoException("O corpo da requisição é obrigatório.");
        }
    }

    private AvaliacaoRequest deserializar(String body) {
        try {
            return objectMapper.readValue(body, AvaliacaoRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidacaoException("JSON inválido.");
        }
    }

    private void logErro(Context context, Exception exception) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log("Erro ao processar avaliação: " + exception.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent error(int status, String message) {
        try {
            return response(status, objectMapper.writeValueAsString(new ErroResponse(message)));
        } catch (Exception e) {
            return response(status, "{\"mensagem\":\"Erro ao gerar resposta.\"}");
        }
    }

    private APIGatewayProxyResponseEvent response(int status, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(JSON_HEADERS)
                .withBody(body);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não configurada: " + name);
        }
        return value;
    }
}
