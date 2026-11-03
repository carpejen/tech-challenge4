package br.fiap.feedback.relatorio.repository;

import br.fiap.feedback.contrato.TabelaAvaliacoes;
import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamoDbAvaliacaoConsultaRepositoryTest {

    @Test
    void converteItemDynamoDbParaDominio() {
        Map<String, AttributeValue> item = Map.of(
                TabelaAvaliacoes.ID, texto("abc"),
                TabelaAvaliacoes.DESCRICAO, texto("Aula com áudio ruim"),
                TabelaAvaliacoes.NOTA, AttributeValue.builder().n("2").build(),
                TabelaAvaliacoes.URGENCIA, texto("CRITICA"),
                TabelaAvaliacoes.DATA_ENVIO, texto("2026-11-01T14:22:10Z"));

        Avaliacao avaliacao = DynamoDbAvaliacaoConsultaRepository.paraAvaliacao(item);

        assertEquals("abc", avaliacao.id());
        assertEquals("Aula com áudio ruim", avaliacao.descricao());
        assertEquals(2, avaliacao.nota());
        assertEquals(Urgencia.CRITICA, avaliacao.urgencia());
        assertEquals("2026-11-01T14:22:10Z", avaliacao.dataEnvio().toString());
    }

    private static AttributeValue texto(String valor) {
        return AttributeValue.builder().s(valor).build();
    }
}
