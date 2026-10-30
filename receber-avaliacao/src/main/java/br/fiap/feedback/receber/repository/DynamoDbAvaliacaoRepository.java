package br.fiap.feedback.receber.repository;

import br.fiap.feedback.contrato.TabelaAvaliacoes;
import br.fiap.feedback.domain.Avaliacao;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Grava avaliações na tabela DynamoDB seguindo o contrato {@link TabelaAvaliacoes}
 * (partição por dia, para o relatório semanal consultar sem {@code Scan}).
 */
@Singleton
public class DynamoDbAvaliacaoRepository implements AvaliacaoRepository {

    private final DynamoDbClient dynamoDb;
    private final String tabela;

    @Inject
    public DynamoDbAvaliacaoRepository(DynamoDbClient dynamoDb,
                                       @ConfigProperty(name = "app.tabela-avaliacoes") String tabela) {
        this.dynamoDb = dynamoDb;
        this.tabela = tabela;
    }

    @Override
    public void salvar(Avaliacao avaliacao) {
        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tabela)
                .item(paraItem(avaliacao))
                // nunca sobrescreve um item existente
                .conditionExpression("attribute_not_exists(" + TabelaAvaliacoes.PK + ")")
                .build());
    }

    static Map<String, AttributeValue> paraItem(Avaliacao avaliacao) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(TabelaAvaliacoes.PK, texto(TabelaAvaliacoes.chaveParticao(avaliacao)));
        item.put(TabelaAvaliacoes.SK, texto(TabelaAvaliacoes.chaveOrdenacao(avaliacao)));
        item.put(TabelaAvaliacoes.ID, texto(avaliacao.id()));
        item.put(TabelaAvaliacoes.DESCRICAO, texto(avaliacao.descricao()));
        item.put(TabelaAvaliacoes.NOTA, AttributeValue.builder().n(Integer.toString(avaliacao.nota())).build());
        item.put(TabelaAvaliacoes.URGENCIA, texto(avaliacao.urgencia().name()));
        item.put(TabelaAvaliacoes.DATA_ENVIO, texto(avaliacao.dataEnvio().toString()));
        return item;
    }

    private static AttributeValue texto(String valor) {
        return AttributeValue.builder().s(valor).build();
    }
}
