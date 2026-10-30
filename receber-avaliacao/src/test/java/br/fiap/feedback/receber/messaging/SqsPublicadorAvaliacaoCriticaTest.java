package br.fiap.feedback.receber.messaging;

import br.fiap.feedback.domain.Avaliacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqsPublicadorAvaliacaoCriticaTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void mensagemTrazOsCamposDoEmailDeUrgencia() throws Exception {
        var publicador = new SqsPublicadorAvaliacaoCritica(null, json, "url-fila");
        Avaliacao avaliacao = Avaliacao.nova("abc", "Áudio ruim", 1, Instant.parse("2026-11-03T14:22:10Z"));

        JsonNode mensagem = json.readTree(publicador.paraJson(avaliacao));

        assertEquals("abc", mensagem.get("id").asText());
        assertEquals("Áudio ruim", mensagem.get("descricao").asText());
        assertEquals(1, mensagem.get("nota").asInt());
        assertEquals("CRITICA", mensagem.get("urgencia").asText());
        assertEquals("2026-11-03T14:22:10Z", mensagem.get("dataEnvio").asText());
    }
}
