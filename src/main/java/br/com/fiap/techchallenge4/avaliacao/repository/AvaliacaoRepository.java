package br.com.fiap.techchallenge4.avaliacao.repository;

import br.com.fiap.techchallenge4.avaliacao.model.Avaliacao;
import br.com.fiap.techchallenge4.avaliacao.model.Urgencia;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvaliacaoRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AvaliacaoRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void save(Avaliacao avaliacao) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", s(avaliacao.id()));
        item.put("descricao", s(avaliacao.descricao()));
        item.put("nota", n(avaliacao.nota()));
        item.put("urgencia", s(avaliacao.urgencia().name()));
        item.put("dataEnvio", s(avaliacao.dataEnvio()));
        item.put("timestampEpoch", n(avaliacao.timestampEpoch()));

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    public List<Avaliacao> findSince(Instant inicio) {
        Map<String, String> names = Map.of("#timestamp", "timestampEpoch");
        Map<String, AttributeValue> values = Map.of(
                ":inicio", AttributeValue.builder().n(String.valueOf(inicio.getEpochSecond())).build()
        );

        List<Avaliacao> resultado = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            ScanRequest.Builder request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("#timestamp >= :inicio")
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values);

            if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
                request.exclusiveStartKey(lastEvaluatedKey);
            }

            var response = dynamoDbClient.scan(request.build());
            response.items().stream().map(this::toModel).forEach(resultado::add);
            lastEvaluatedKey = response.lastEvaluatedKey();
        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        return resultado;
    }

    private Avaliacao toModel(Map<String, AttributeValue> item) {
        return new Avaliacao(
                item.get("id").s(),
                item.get("descricao").s(),
                Integer.parseInt(item.get("nota").n()),
                Urgencia.valueOf(item.get("urgencia").s()),
                item.get("dataEnvio").s(),
                Long.parseLong(item.get("timestampEpoch").n())
        );
    }

    private AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private AttributeValue n(long value) {
        return AttributeValue.builder().n(String.valueOf(value)).build();
    }
}
