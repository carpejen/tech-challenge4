package br.fiap.feedback.receber.function;

import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.receber.messaging.PublicadorAvaliacaoCritica;
import br.fiap.feedback.receber.repository.AvaliacaoRepository;
import br.fiap.feedback.receber.service.AvaliacaoService;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceberAvaliacaoHandlerTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<Avaliacao> gravadas = new ArrayList<>();
    private final List<Avaliacao> publicadas = new ArrayList<>();
    private boolean bancoFora;

    private final AvaliacaoRepository repositorio = avaliacao -> {
        if (bancoFora) {
            throw new IllegalStateException("DynamoDB indisponível");
        }
        gravadas.add(avaliacao);
    };
    private final PublicadorAvaliacaoCritica publicador = publicadas::add;
    private final ReceberAvaliacaoHandler handler = new ReceberAvaliacaoHandler(
            new AvaliacaoService(repositorio, publicador,
                    Clock.fixed(Instant.parse("2026-11-03T14:22:10Z"), ZoneOffset.UTC)),
            json);

    @Test
    void avaliacaoValidaRetorna201ComUrgencia() throws Exception {
        APIGatewayProxyResponseEvent resposta = enviar("{\"descricao\":\"Aula com áudio ruim\",\"nota\":2}");

        assertEquals(201, resposta.getStatusCode());
        assertTrue(resposta.getHeaders().get("Content-Type").startsWith("application/json"));
        JsonNode corpo = json.readTree(resposta.getBody());
        assertEquals("CRITICA", corpo.get("urgencia").asText());
        assertEquals(2, corpo.get("nota").asInt());
        assertEquals("2026-11-03T14:22:10Z", corpo.get("dataEnvio").asText());
        assertEquals(gravadas.get(0).id(), corpo.get("id").asText());
        assertEquals(1, publicadas.size());
    }

    @Test
    void avaliacaoNaoCriticaNaoVaiParaAFila() {
        assertEquals(201, enviar("{\"descricao\":\"Muito boa\",\"nota\":10}").getStatusCode());
        assertEquals(1, gravadas.size());
        assertTrue(publicadas.isEmpty());
    }

    @Test
    void entradasInvalidasRetornam400() throws Exception {
        String[] invalidas = {
                null,
                "",
                "isso não é json",
                "[1,2]",
                "{\"nota\":5}",
                "{\"descricao\":\"   \",\"nota\":5}",
                "{\"descricao\":\"ok\"}",
                "{\"descricao\":\"ok\",\"nota\":11}",
                "{\"descricao\":\"ok\",\"nota\":-1}",
                "{\"descricao\":\"ok\",\"nota\":7.5}",
                "{\"descricao\":\"ok\",\"nota\":\"7\"}",
                "{\"descricao\":123,\"nota\":7}",
        };
        for (String corpo : invalidas) {
            APIGatewayProxyResponseEvent resposta = enviar(corpo);
            assertEquals(400, resposta.getStatusCode(), "corpo: " + corpo);
            assertTrue(json.readTree(resposta.getBody()).hasNonNull("erro"), "corpo: " + corpo);
        }
        assertTrue(gravadas.isEmpty());
    }

    @Test
    void falhaNoBancoRetorna500SemExporDetalhes() throws Exception {
        bancoFora = true;

        APIGatewayProxyResponseEvent resposta = enviar("{\"descricao\":\"ok\",\"nota\":5}");

        assertEquals(500, resposta.getStatusCode());
        String erro = json.readTree(resposta.getBody()).get("erro").asText();
        assertTrue(!erro.contains("DynamoDB"), "não deve vazar detalhes internos: " + erro);
    }

    private APIGatewayProxyResponseEvent enviar(String corpo) {
        return handler.handleRequest(new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(corpo), null);
    }
}
