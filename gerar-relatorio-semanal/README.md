# Lambda `gerar-relatorio-semanal` (próxima etapa)

| | |
|---|---|
| **Gatilho** | EventBridge Scheduler: `cron(0 8 ? * MON *)` com fuso `America/Sao_Paulo` |
| **Responsabilidade** | Consolidar as avaliações dos últimos 7 dias e enviar o relatório |
| **Leitura** | Tabela de avaliações: 7 `Query`, uma por dia, com `TabelaAvaliacoes.chaveParticao(LocalDate)` do módulo `shared` |
| **Saída** | E-mail com média das notas, quantidade por dia, quantidade por urgência e a lista (descrição, urgência, data de envio) |

## Como criar este módulo

1. Copie o `pom.xml` de `receber-avaliacao/`, troque o `artifactId` e deixe só as dependências
   que esta Lambda usa (DynamoDB e e-mail).
2. Crie o handler em `src/main/java/br/fiap/feedback/relatorio/function/`:
   `@Named("relatorio")`, implementando `RequestHandler<Map<String, Object>, Void>`.
3. Em `src/main/resources/application.properties`: `quarkus.lambda.handler=relatorio`.
4. No `pom.xml` da raiz, descomente `<module>gerar-relatorio-semanal</module>`.
5. No `template.yaml`, adicione a função com
   `CodeUri: gerar-relatorio-semanal/target/function.zip`, o evento `Type: ScheduleV2`
   e permissão só de `dynamodb:Query` na tabela (mais a de e-mail).
