package br.fiap.feedback.receber.function;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.AvaliacaoInvalidaException;
import br.fiap.feedback.receber.service.AvaliacaoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Função Lambda {@code receber-avaliacao}.
 *
 * <p><b>Gatilho:</b> API Gateway, {@code POST /avaliacao} (exige API key).<br>
 * <b>Responsabilidade:</b> traduzir a requisição HTTP numa chamada ao {@link AvaliacaoService}
 * e a resposta do serviço num HTTP 201/400/500. Nenhuma regra de negócio mora aqui.
 *
 * <p>Exemplo de corpo: {@code {"descricao": "Áudio da aula ruim", "nota": 2}}
 */
@Named("receber")
@Dependent
public class ReceberAvaliacaoHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOG = Logger.getLogger(ReceberAvaliacaoHandler.class);
    private static final Map<String, String> CABECALHOS = Map.of("Content-Type", "application/json; charset=utf-8");

    private final AvaliacaoService service;
    private final ObjectMapper json;

    @Inject
    public ReceberAvaliacaoHandler(AvaliacaoService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requisicao, Context contexto) {
        try {
            AvaliacaoRequest entrada = lerEntrada(requisicao == null ? null : requisicao.getBody());
            Avaliacao registrada = service.registrar(entrada.descricao(), entrada.nota());
            return resposta(201, AvaliacaoResponse.de(registrada));
        } catch (AvaliacaoInvalidaException e) {
            return resposta(400, new ErroResponse(e.getMessage()));
        } catch (RuntimeException e) {
            LOG.error("Erro inesperado ao registrar avaliacao", e);
            return resposta(500, new ErroResponse("erro interno ao registrar a avaliação"));
        }
    }

    /**
     * Lê o JSON de entrada com validação de tipos: {@code descricao} precisa ser texto e
     * {@code nota} precisa ser um inteiro (7.5 ou "7" são rejeitados, em vez de convertidos).
     */
    AvaliacaoRequest lerEntrada(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            throw new AvaliacaoInvalidaException("corpo da requisição é obrigatório");
        }
        JsonNode raiz;
        try {
            raiz = json.readTree(corpo);
        } catch (JsonProcessingException e) {
            throw new AvaliacaoInvalidaException("JSON inválido");
        }
        if (raiz == null || !raiz.isObject()) {
            throw new AvaliacaoInvalidaException("o corpo deve ser um objeto JSON");
        }

        JsonNode descricao = raiz.get("descricao");
        JsonNode nota = raiz.get("nota");
        if (presente(descricao) && !descricao.isTextual()) {
            throw new AvaliacaoInvalidaException("descricao deve ser texto");
        }
        if (presente(nota) && !(nota.isIntegralNumber() && nota.canConvertToInt())) {
            throw new AvaliacaoInvalidaException("nota deve ser um número inteiro");
        }
        return new AvaliacaoRequest(
                presente(descricao) ? descricao.asText() : null,
                presente(nota) ? nota.asInt() : null);
    }

    private static boolean presente(JsonNode no) {
        return no != null && !no.isNull();
    }

    private APIGatewayProxyResponseEvent resposta(int status, Object corpo) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(CABECALHOS)
                    .withBody(json.writeValueAsString(corpo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar a resposta", e);
        }
    }
}
