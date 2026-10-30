# Lambda `notificar-urgencia` (próxima etapa)

| | |
|---|---|
| **Gatilho** | Fila SQS `feedback-serverless-avaliacoes-criticas` (batch de 1 mensagem) |
| **Responsabilidade** | Avisar os administradores sobre uma avaliação crítica |
| **Entrada** | `br.fiap.feedback.contrato.MensagemAvaliacaoCritica` (módulo `shared`) |
| **Saída** | E-mail com descrição, urgência e data de envio |
| **Falhas** | Exceção devolve a mensagem para a fila; depois de 3 tentativas ela vai para a DLQ |

## Como criar este módulo

1. Copie o `pom.xml` de `receber-avaliacao/`, troque o `artifactId` e deixe só as dependências
   que esta Lambda usa (a de e-mail, SNS ou SES, conforme a decisão do grupo, em vez de DynamoDB e SQS).
2. Crie o handler em `src/main/java/br/fiap/feedback/notificar/function/`:
   `@Named("notificar")`, implementando `RequestHandler<SQSEvent, Void>`.
3. Em `src/main/resources/application.properties`: `quarkus.lambda.handler=notificar`.
4. No `pom.xml` da raiz, descomente `<module>notificar-urgencia</module>`.
5. No `template.yaml`, adicione a função com
   `CodeUri: notificar-urgencia/target/function.zip`, o evento `Type: SQS` apontando para
   `FilaCriticas` e as permissões de envio de e-mail.
