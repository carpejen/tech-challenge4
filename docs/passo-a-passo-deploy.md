# Passo a passo: do código no GitHub até a Lambda rodando na AWS

Este guia mostra como a função `receber-avaliacao` sai do repositório e vira uma Lambda
funcionando na AWS, e como repetir isso a cada mudança.

## 0. Como o código chega na Lambda

A Lambda **não busca o código no GitHub sozinha**. Quem leva o código até ela é o
**pipeline** (GitHub Actions) usando o **AWS SAM**:

```mermaid
flowchart LR
    DEV["merge do PR na main"] --> GHA["GitHub Actions<br/>mvn verify"]
    GHA -->|"gera um zip por Lambda"| ZIP["receber-avaliacao/target/function.zip<br/>(+ os das próximas Lambdas)"]
    GHA -->|"login via OIDC<br/>(sem access key)"| SAM["sam deploy"]
    ZIP --> SAM
    SAM -->|1. envia os zips| S3[("Bucket S3<br/>do SAM")]
    SAM -->|2. envia o template.yaml| CFN["CloudFormation<br/>stack feedback-serverless"]
    CFN -->|cria / atualiza| REC["Lambda · API Gateway<br/>DynamoDB · SQS · Logs"]
    S3 -. código da Lambda .-> REC
```

1. O `mvn verify` na raiz compila e testa todos os módulos, e gera um pacote por Lambda
   (`receber-avaliacao/target/function.zip` etc.).
2. O GitHub Actions faz login na AWS assumindo uma role IAM via **OIDC**. É um token
   temporário emitido pelo GitHub, então nenhuma senha ou access key fica guardada.
3. O `sam deploy` sobe os zips para um bucket S3 e entrega o `template.yaml` ao
   **CloudFormation**, que cria ou atualiza todos os recursos: Lambda, API Gateway,
   tabela, filas e logs. Esse conjunto de recursos é a **stack** `feedback-serverless`.
4. A cada merge na `main` o ciclo se repete. O CloudFormation só altera o que mudou.

> A `main` é protegida: o código entra por pull request. No PR o pipeline só compila e testa.
> O deploy acontece quando o PR é aceito (merge na `main`).

---

## 1. Pré-requisitos

| O quê | Para quê |
|---|---|
| Acesso de **admin** ao repositório no GitHub | Cadastrar o secret do deploy (passo 4) |
| Acesso ao projeto AWS `tech-challenge-4-fiap-adj` (região **us-east-2**, Ohio) | Criar a role de deploy (passo 3) e conferir os recursos |
| Opcional: Java 21 + Maven 3.9 na máquina | Rodar os testes localmente (`mvn verify`) |

Não é preciso instalar AWS CLI nem SAM CLI na sua máquina: o deploy roda no GitHub Actions.

---

## 2. Subir o código por pull request

```bash
git checkout -b feat/receber-avaliacao
git add .
git commit -m "feat: lambda receber-avaliacao com API Gateway, DynamoDB e SQS"
git push -u origin feat/receber-avaliacao
```

No GitHub, abra o pull request da branch para a `main`. O workflow `ci-cd` roda o job
**Build e testes**, que precisa ficar verde antes do merge.

Depois do merge, o workflow roda de novo na `main`. Até os passos 3 e 4 serem feitos, o job
**Deploy na AWS** termina com um aviso de que o secret `AWS_DEPLOY_ROLE_ARN` não está configurado.
É esperado.

---

## 3. Criar a role de deploy na AWS (uma vez só)

O arquivo `infra/github-oidc.yaml` cria duas coisas:

- o **provedor OIDC** do GitHub, que faz a AWS confiar nos tokens emitidos pelo GitHub Actions;
- a role **`github-deploy-feedback-serverless`**, que só pode ser assumida por
  `carpejen/tech-challenge4` na branch `main`.

Pelo console da AWS:

1. Abra o **CloudFormation** (confira que a região no canto superior é **us-east-2**).
2. **Criar pilha** (Create stack) → **Com novos recursos (padrão)**.
3. **Fazer upload de um arquivo de modelo** → escolha `infra/github-oidc.yaml` → Próximo.
4. Nome da pilha: `github-oidc`. Confira os parâmetros (`carpejen`, `tech-challenge4`, `main`)
   e avance com os valores padrão.
5. Na revisão, marque a caixa **"Eu reconheço que o AWS CloudFormation pode criar recursos do IAM
   com nomes personalizados"** e clique em **Enviar**.
6. Espere o status **CREATE_COMPLETE**. Na aba **Saídas** (Outputs), copie o valor de **RoleArn**.
   Ele tem este formato: `arn:aws:iam::481737577322:role/github-deploy-feedback-serverless`.

> Se der erro dizendo que o provedor `token.actions.githubusercontent.com` já existe, apague a pilha
> com falha e crie de novo com `CriarProvedorOidc = nao`, preenchendo o ARN do provedor existente
> (IAM → Provedores de identidade).

**Por que essa role é segura o bastante:** ela tem `PowerUserAccess` (cria Lambda, API, tabelas etc.,
mas não mexe em IAM) e só pode gerenciar roles que começam com `feedback-serverless-`, que são as
roles das Lambdas. O nome dela começa com `github-deploy-` de propósito, para ela não conseguir
alterar a si mesma.

---

## 4. Cadastrar o secret no GitHub

No repositório: **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

- **Name:** `AWS_DEPLOY_ROLE_ARN`
- **Secret:** o RoleArn copiado no passo 3

> Só quem é admin do repositório vê o menu Settings. Se você não for, peça para a dona
> do repositório (carpejen) cadastrar.

---

## 5. Fazer o deploy

Rode de novo o workflow da `main`: aba **Actions** → `ci-cd` → última execução da `main` →
**Re-run all jobs**. Também funciona fazer o merge de qualquer novo PR.

Acompanhe em dois lugares:

- **GitHub Actions:** o job *Deploy na AWS* mostra o `sam deploy` listando o que vai criar.
- **AWS CloudFormation:** a stack `feedback-serverless` aparece, e a aba **Eventos** mostra cada
  recurso sendo criado. No fim o status deve ser **CREATE_COMPLETE** (ou **UPDATE_COMPLETE** nos
  deploys seguintes).

O SAM também cria uma stack auxiliar chamada `aws-sam-cli-managed-default`, com o bucket S3 onde
ele guarda os pacotes das Lambdas. Isso é normal.

---

## 6. Pegar a URL e a API key

Na stack `feedback-serverless`, aba **Saídas** (Outputs):

| Saída | O que é |
|---|---|
| `UrlAvaliacao` | Endpoint do `POST /avaliacao` |
| `ApiKeyId` | ID da API key criada pelo SAM |

Para ver o **valor** da API key: **API Gateway** → **Chaves de API** (API keys) → clique na chave →
**Mostrar**. Pela CLI, o comando equivalente é
`aws apigateway get-api-key --include-value --api-key <ApiKeyId>`.

> Não coloque o valor da API key no repositório (ele é público). Use só no Postman ou no curl.

---

## 7. Testar

```bash
URL="<UrlAvaliacao>"
KEY="<valor da API key>"

# Nota alta → 201, urgência BAIXA, só grava
curl -i -X POST "$URL" -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"descricao":"Aula muito boa","nota":9}'

# Nota baixa → 201, urgência CRITICA, grava e manda para a fila
curl -i -X POST "$URL" -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"descricao":"Audio da aula ruim","nota":2}'

# Sem API key → 403 (barrado no API Gateway, a Lambda nem é chamada)
curl -i -X POST "$URL" -H "Content-Type: application/json" -d '{"descricao":"x","nota":5}'

# Nota inválida → 400
curl -i -X POST "$URL" -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"descricao":"x","nota":11}'
```

A primeira chamada pode demorar alguns segundos. É o *cold start* do Java; as chamadas seguintes são rápidas.

---

## 8. Conferir na AWS (bom para o vídeo)

| Onde | O que ver |
|---|---|
| **DynamoDB** → Tabelas → `feedback-serverless-avaliacoes` → **Explorar itens** | As avaliações gravadas, com `pk = DIA#...` |
| **SQS** → `feedback-serverless-avaliacoes-criticas` → **Enviar e receber mensagens** → **Pesquisar mensagens** | A mensagem da avaliação crítica (nota 2) |
| **CloudWatch** → Grupos de logs → `/aws/lambda/feedback-serverless-receber-avaliacao` | Logs `Avaliacao registrada id=... urgencia=...` (a descrição não é logada) |
| **X-Ray** / CloudWatch → Rastreamentos | O caminho API Gateway → Lambda → DynamoDB/SQS |
| **Lambda** → `feedback-serverless-receber-avaliacao` → Configuração | Variáveis de ambiente, role e permissões |

---

## 9. Atualizar a Lambda depois

1. Crie uma branch, altere o código e abra um pull request. O pipeline roda os testes.
2. Faça o merge na `main`. O pipeline publica a nova versão.

Não tem passo manual: **o código no GitHub (main) é sempre o que está na AWS**.

---

## 10. Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Job de deploy com aviso "secret não configurado" | Faltou o passo 4 |
| `Not authorized to perform sts:AssumeRoleWithWebIdentity` | Dono, repositório ou branch diferentes dos parâmetros da pilha `github-oidc` |
| CloudFormation `ROLLBACK_COMPLETE` | Veja o primeiro evento em vermelho na aba Eventos. Depois apague a stack antes de tentar de novo |
| `403 Forbidden` no curl | Header `x-api-key` ausente ou com valor errado |
| `500` na resposta | Veja o log da Lambda no CloudWatch |
| `429 Too Many Requests` | Limite do plano de uso (5 req/s, pico de 10) |

---

## 11. Remover tudo (depois da avaliação)

No CloudFormation, apague nesta ordem:

1. `feedback-serverless` (Lambda, API, tabela, filas e logs);
2. `aws-sam-cli-managed-default` (esvazie o bucket S3 dela antes);
3. `github-oidc` (role de deploy e provedor OIDC).
