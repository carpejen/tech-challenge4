package br.fiap.feedback.relatorio.repository;

import br.fiap.feedback.contrato.TabelaAvaliacoes;
import br.fiap.feedback.domain.Avaliacao;
import br.fiap.feedback.domain.Urgencia;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consulta avaliações no DynamoDB usando a partição diária definida por {@link TabelaAvaliacoes}.
 * O relatório semanal executa sete Query, uma para cada dia, evitando Scan da tabela inteira.
 */
@Singleton
public class DynamoDbAvaliacaoConsultaRepository implements AvaliacaoConsultaRepository {

    private final DynamoDbClient dynamoDb;
    private final String tabela;

    @Inject
    public DynamoDbAvaliacaoConsultaRepository(DynamoDbClient dynamoDb,
                                                @ConfigProperty(name = "app.tabela-avaliacoes") String tabela) {
        this.dynamoDb = dynamoDb;
        this.tabela = tabela;
    }

    @Override
    public List<Avaliacao> buscarPorDia(LocalDate dia) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        Map<String, AttributeValue> proximaChave = Map.of();

        do {
            QueryRequest.Builder consulta = QueryRequest.builder()
                    .tableName(tabela)
                    .keyConditionExpression("#pk = :pk")
                    .expressionAttributeNames(Map.of("#pk", TabelaAvaliacoes.PK))
                    .expressionAttributeValues(Map.of(
                            ":pk", AttributeValue.builder()
                                    .s(TabelaAvaliacoes.chaveParticao(dia))
                                    .build()));

            if (!proximaChave.isEmpty()) {
                consulta.exclusiveStartKey(proximaChave);
            }

            QueryResponse resposta = dynamoDb.query(consulta.build());
            resposta.items().stream().map(DynamoDbAvaliacaoConsultaRepository::paraAvaliacao).forEach(avaliacoes::add);
            proximaChave = resposta.lastEvaluatedKey();
        } while (proximaChave != null && !proximaChave.isEmpty());

        return avaliacoes;
    }

    static Avaliacao paraAvaliacao(Map<String, AttributeValue> item) {
        return new Avaliacao(
                texto(item, TabelaAvaliacoes.ID),
                texto(item, TabelaAvaliacoes.DESCRICAO),
                Integer.parseInt(numero(item, TabelaAvaliacoes.NOTA)),
                Urgencia.valueOf(texto(item, TabelaAvaliacoes.URGENCIA)),
                Instant.parse(texto(item, TabelaAvaliacoes.DATA_ENVIO)));
    }

    private static String texto(Map<String, AttributeValue> item, String atributo) {
        AttributeValue valor = item.get(atributo);
        if (valor == null || valor.s() == null) {
            throw new IllegalStateException("Atributo ausente no DynamoDB: " + atributo);
        }
        return valor.s();
    }

    private static String numero(Map<String, AttributeValue> item, String atributo) {
        AttributeValue valor = item.get(atributo);
        if (valor == null || valor.n() == null) {
            throw new IllegalStateException("Atributo ausente no DynamoDB: " + atributo);
        }
        return valor.n();
    }
}
