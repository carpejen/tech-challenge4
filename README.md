# Tech Challenge 4 - Plataforma de Feedback

Estrutura inicial em Java 21 + Maven + AWS SAM para uma aplicação serverless de feedbacks.

## Arquitetura inicial

- API Gateway: `POST /avaliacao`
- Lambda `ReceberAvaliacaoHandler`: valida e persiste feedbacks no DynamoDB
- SNS `CriticalFeedbackTopic`: desacopla feedback crítico da notificação
- Lambda `NotificarAvaliacaoCriticaHandler`: formata e encaminha o alerta
- SNS `AdminNotificationTopic`: entrega e-mail ao administrador
- EventBridge: agenda o relatório semanal
- Lambda `GerarRelatorioSemanalHandler`: calcula estatísticas e envia relatório
- DynamoDB: armazena os feedbacks
- CloudWatch/X-Ray: logs e tracing das funções

## Regra de urgência adotada

O enunciado usa o campo "urgência", mas não define sua derivação. Nesta implementação inicial foi adotada a seguinte decisão de projeto:

- nota 0 a 3: `CRITICA`
- nota 4 a 6: `MEDIA`
- nota 7 a 10: `BAIXA`

Essa regra deve ser validada pelo grupo/professor e documentada na entrega.

## Build

```bash
mvn clean test package
```

## AWS SAM

Pré-requisitos:

- AWS CLI configurada
- AWS SAM CLI
- Java 21
- Maven

Verifique se o JDK ativo é o 21:

```bash
java -version
```

O Maven Enforcer também bloqueia o build caso o projeto seja executado com outra versão do Java.

Validar o template:

```bash
sam validate
```

Build:

```bash
sam build
```

Deploy guiado:

```bash
sam deploy --guided
```

Durante o deploy, informe o parâmetro `AdminEmail`. A AWS enviará um e-mail de confirmação da assinatura SNS; a assinatura precisa ser confirmada.

## Exemplo de requisição

```http
POST /avaliacao
Content-Type: application/json

{
  "descricao": "A aula apresentou problemas no áudio",
  "nota": 2
}
```

## Próximos passos

1. Adicionar testes dos handlers e do relatório.
2. Melhorar observabilidade com alarmes CloudWatch.
3. Definir autenticação/autorização da API conforme a estratégia de segurança do grupo.
4. Criar pipeline de CI/CD (GitHub Actions + SAM).
5. Documentar arquitetura, segurança, monitoramento e deploy para a entrega final.


## Build reproduzível com Java 21

Para garantir que o build do SAM use o mesmo ambiente do runtime Java 21 da AWS Lambda, prefira:

```bash
sam validate
sam build --use-container
```

Com `Runtime: java21`, o AWS SAM seleciona o workflow Maven para Java e, com `--use-container`, usa a imagem de build correspondente ao Java 21.


## Build Java 21 com AWS SAM

Este projeto usa o fluxo nativo de build Java/Maven do AWS SAM. Por isso, não é necessário gerar um *uber/fat JAR* com `maven-shade-plugin`: o `sam build` compila o projeto e monta o artefato de cada Lambda com as dependências necessárias.

Validação local recomendada:

```bash
java -version
mvn -version
mvn clean test
sam validate --lint
sam build --use-container
```

O JDK e o Maven usados localmente devem apontar para Java 21. O `maven-enforcer-plugin` interrompe o build quando o Maven estiver executando fora da faixa Java 21.

Para este projeto, prefira `sam build --use-container` antes do deploy, pois o build é executado em um ambiente compatível com o runtime `java21` configurado nas funções.

> Observação: após o deploy, a assinatura de e-mail criada pelo SNS precisa ser confirmada no endereço informado em `AdminEmail` antes que os alertas e relatórios sejam entregues.


## Testes do endpoint POST /avaliacao

O `ReceberAvaliacaoHandlerTest` cobre o contrato HTTP e os principais cenários de erro do endpoint:

- sucesso (`201`);
- descrição ausente ou em branco (`400`);
- nota ausente, menor que 0 ou maior que 10 (`400`);
- JSON inválido (`400`);
- corpo vazio, nulo ou evento nulo (`400`);
- avaliação crítica com publicação do evento;
- falha de persistência simulada (`500`).

Execute localmente com:

```bash
mvn clean test
```


## Execução local sem recursos AWS

O projeto está preparado para execução com **AWS SAM + Docker + LocalStack**.

Arquivos principais:

- `docker-compose.yml`: sobe DynamoDB, SNS e SQS locais via LocalStack;
- `env.local.json`: variáveis usadas pelas Lambdas locais;
- `events/`: eventos de API Gateway, SNS e EventBridge;
- `requests/local.http`: chamadas HTTP para o IntelliJ;
- `LOCAL-DEVELOPMENT.md`: roteiro completo de execução local.

Fluxo resumido:

```bash
docker compose up -d
mvn clean test
sam validate --lint
sam build --use-container
sam local start-api --env-vars env.local.json --docker-network tc4-local
```

Depois, envie um `POST` para `http://localhost:3000/avaliacao`. Consulte `LOCAL-DEVELOPMENT.md` para testar as três Lambdas e inspecionar DynamoDB/SNS localmente.
