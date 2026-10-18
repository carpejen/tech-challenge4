package br.com.fiap.techchallenge4.avaliacao.handler;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;
import br.com.fiap.techchallenge4.avaliacao.publisher.CriticalEventPublisher;
import br.com.fiap.techchallenge4.avaliacao.repository.AvaliacaoRepository;
import br.com.fiap.techchallenge4.avaliacao.service.AvaliacaoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReceberAvaliacaoHandlerTest {

    private static final Instant DATA_FIXA = Instant.parse("2026-09-11T18:00:00Z");

    @Mock
    private AvaliacaoRepository repository;

    @Mock
    private CriticalEventPublisher publisher;

    @Mock
    private Context context;

    private ObjectMapper objectMapper;
    private ReceberAvaliacaoHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(DATA_FIXA, ZoneOffset.UTC);
        AvaliacaoService service = new AvaliacaoService(repository, publisher, clock);
        handler = new ReceberAvaliacaoHandler(objectMapper, service);
    }

    @Test
    void deveRetornar201QuandoAvaliacaoForValida() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "Excelente aula sobre cloud",
                  "nota": 9
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(201, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));

        JsonNode body = objectMapper.readTree(response.getBody());
        assertFalse(body.get("id").asText().isBlank());
        assertEquals("Excelente aula sobre cloud", body.get("descricao").asText());
        assertEquals(9, body.get("nota").asInt());
        assertEquals("BAIXA", body.get("urgencia").asText());
        assertEquals("2026-09-11T18:00Z", body.get("dataEnvio").asText());

        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        verify(repository).save(captor.capture());
        assertEquals(body.get("id").asText(), captor.getValue().id());
        verify(publisher, never()).publish(captor.getValue());
    }

    @Test
    void deveRetornar400QuandoDescricaoObrigatoriaEstiverAusente() throws Exception {
        var event = eventoComBody("""
                {
                  "nota": 8
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "A descrição é obrigatória.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoDescricaoForVazia() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "   ",
                  "nota": 8
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "A descrição é obrigatória.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoNotaObrigatoriaEstiverAusente() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "Aula sem nota"
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "A nota deve estar entre 0 e 10.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoNotaForMenorQueZero() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "Nota inválida",
                  "nota": -1
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "A nota deve estar entre 0 e 10.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoNotaForMaiorQueDez() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "Nota inválida",
                  "nota": 11
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "A nota deve estar entre 0 e 10.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoJsonForInvalido() throws Exception {
        var event = eventoComBody("{\"descricao\":\"Aula\",\"nota\":}");

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "JSON inválido.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoCorpoEstiverVazio() throws Exception {
        var event = eventoComBody("   ");

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "O corpo da requisição é obrigatório.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoBodyForNulo() throws Exception {
        var event = eventoComBody(null);

        var response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "O corpo da requisição é obrigatório.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void deveRetornar400QuandoEventoForNulo() throws Exception {
        var response = handler.handleRequest(null, context);

        assertEquals(400, response.getStatusCode());
        assertMensagemErro(response.getBody(), "O corpo da requisição é obrigatório.");
        verifyNoInteractions(repository, publisher);
    }

    @Test
    void devePersistirEPublicarEventoQuandoAvaliacaoForCritica() throws Exception {
        var event = eventoComBody("""
                {
                  "descricao": "Aula com problema grave de áudio",
                  "nota": 2
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(201, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("CRITICA", body.get("urgencia").asText());
        assertEquals(2, body.get("nota").asInt());

        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        verify(repository).save(captor.capture());

        Avaliacao avaliacaoPersistida = captor.getValue();
        assertEquals(Urgencia.CRITICA, avaliacaoPersistida.urgencia());
        assertEquals("Aula com problema grave de áudio", avaliacaoPersistida.descricao());
        assertEquals(2, avaliacaoPersistida.nota());
        verify(publisher).publish(avaliacaoPersistida);
    }

    @Test
    void deveRetornar500QuandoOcorrerErroAoPersistir() throws Exception {
        doThrow(new RuntimeException("DynamoDB indisponível"))
                .when(repository)
                .save(any(Avaliacao.class));

        var event = eventoComBody("""
                {
                  "descricao": "Aula válida",
                  "nota": 8
                }
                """);

        var response = handler.handleRequest(event, context);

        assertEquals(500, response.getStatusCode());
        assertMensagemErro(response.getBody(), "Erro interno ao processar a avaliação.");
        verify(repository).save(any(Avaliacao.class));
        verifyNoInteractions(publisher);
    }

    private APIGatewayProxyRequestEvent eventoComBody(String body) {
        return new APIGatewayProxyRequestEvent().withBody(body);
    }

    private void assertMensagemErro(String responseBody, String mensagemEsperada) throws Exception {
        JsonNode body = objectMapper.readTree(responseBody);
        assertEquals(mensagemEsperada, body.get("mensagem").asText());
    }
}
