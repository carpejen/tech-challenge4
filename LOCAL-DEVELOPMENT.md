# Execução local: AWS SAM + Docker + LocalStack

Esta configuração permite testar o Tech Challenge 4 sem criar recursos na AWS.

## O que roda localmente

- AWS SAM executa as Lambdas Java 21 em containers Docker.
- LocalStack fornece DynamoDB, SNS e SQS localmente.
- SQS é usado somente em desenvolvimento como caixa de observação das mensagens publicadas nos tópicos SNS.
- `env.local.json` faz as Lambdas apontarem para `http://localstack:4566`.

O SQS **não faz parte da arquitetura de produção** deste projeto.

## 1. Pré-requisitos

Instale e deixe disponíveis no PATH:

- Java 21
- Maven
- Docker Desktop
- AWS SAM CLI

Confirme:

```bash
java -version
mvn -version
docker version
sam --version
```

## 2. Subir os serviços locais

Na raiz do projeto:

```bash
docker compose up -d
```

Aguarde até o container `tech-challenge-4-localstack` ficar `healthy`:

```bash
docker compose ps
```

O script `localstack/init/ready.d/01-init.sh` cria automaticamente:

- tabela `tech-challenge-4-feedbacks`;
- tópico `tech-challenge-4-critical-feedbacks`;
- tópico `tech-challenge-4-admin-notifications`;
- duas filas SQS locais para observar mensagens publicadas.

Para acompanhar a inicialização:

```bash
docker compose logs -f localstack
```

## 3. Testes unitários e build SAM

```bash
mvn clean test
sam validate --lint
sam build --use-container
```

## 4. Invocar a Lambda de entrada diretamente

Avaliação válida:

```bash
sam local invoke ReceberAvaliacaoFunction \
  --event events/avaliacao-sucesso.json \
  --env-vars env.local.json \
  --docker-network tc4-local
```

Avaliação crítica:

```bash
sam local invoke ReceberAvaliacaoFunction \
  --event events/avaliacao-critica.json \
  --env-vars env.local.json \
  --docker-network tc4-local
```

Entradas inválidas:

```bash
sam local invoke ReceberAvaliacaoFunction \
  --event events/avaliacao-invalida.json \
  --env-vars env.local.json \
  --docker-network tc4-local

sam local invoke ReceberAvaliacaoFunction \
  --event events/avaliacao-json-invalido.json \
  --env-vars env.local.json \
  --docker-network tc4-local
```

## 5. Subir a API local

```bash
sam local start-api \
  --env-vars env.local.json \
  --docker-network tc4-local
```

A API ficará em:

```text
http://localhost:3000/avaliacao
```

Pode usar o arquivo `requests/local.http` no IntelliJ para disparar os requests.

Com curl:

```bash
curl -i -X POST http://localhost:3000/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Excelente aula sobre AWS","nota":9}'
```

## 6. Conferir os dados gravados no DynamoDB local

```bash
docker exec tech-challenge-4-localstack \
  awslocal dynamodb scan \
  --table-name tech-challenge-4-feedbacks
```

Ou execute:

```powershell
./scripts/local-status.ps1
```

No Git Bash/Linux/macOS:

```bash
./scripts/local-status.sh
```

## 7. Conferir publicação de avaliação crítica

Depois de enviar uma avaliação com nota de 0 a 3, a função publica no tópico SNS local.

A fila `tech-challenge-4-critical-events` recebe uma cópia da mensagem apenas para facilitar a inspeção local. No Windows, use:

```powershell
./scripts/local-status.ps1
```

No Git Bash/Linux/macOS:

```bash
./scripts/local-status.sh
```

## 8. Testar a Lambda de notificação crítica

O SAM local não conecta automaticamente o tópico SNS do LocalStack a uma Lambda executada pelo `sam local invoke`. Por isso, esta função é testada com um evento SNS de exemplo:

```bash
sam local invoke NotificarAvaliacaoCriticaFunction \
  --event events/sns-avaliacao-critica.json \
  --env-vars env.local.json \
  --docker-network tc4-local
```

A mensagem enviada ao tópico administrativo pode ser conferida executando novamente `scripts/local-status.ps1` (Windows) ou `scripts/local-status.sh` (Git Bash/Linux/macOS).

## 9. Testar o relatório semanal

Primeiro grave algumas avaliações usando o endpoint local. Depois execute:

```bash
sam local invoke GerarRelatorioSemanalFunction \
  --event events/eventbridge-relatorio-semanal.json \
  --env-vars env.local.json \
  --docker-network tc4-local
```

A saída do relatório é publicada no tópico administrativo e pode ser vista na fila `tech-challenge-4-admin-messages`.

## 10. Encerrar o ambiente

Parar os containers preservando o volume:

```bash
docker compose down
```

Parar e apagar também os dados locais:

```bash
docker compose down -v
```

## Por que existe `AwsClientFactory`?

Em produção, as Lambdas não terão `AWS_ENDPOINT_URL`; portanto os clientes do AWS SDK usam normalmente o endpoint real da AWS e as credenciais da role da Lambda.

Em desenvolvimento, `env.local.json` define:

```text
AWS_ENDPOINT_URL=http://localstack:4566
```

Assim os mesmos handlers utilizam o LocalStack sem colocar condicionais de ambiente nas regras de negócio e sem hardcode de endpoint nas classes de serviço/repositório.
