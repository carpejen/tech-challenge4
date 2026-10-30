# 8. Glossário

[← Anterior: perguntas frequentes](07-perguntas-frequentes.md) · [Índice](README.md)

| Termo | Em uma linha |
|---|---|
| **Access key** | Par de credenciais (ID + chave secreta) que um programa usa para chamar a AWS. A nossa fica só nos Secrets do GitHub |
| **API Gateway** | Serviço que publica a API HTTPS e encaminha as requisições para a Lambda |
| **API key** | Chave exigida no header `x-api-key` para chamar a nossa API |
| **Artifact** | Arquivo que um job do GitHub Actions passa para outro (os `function.zip`) |
| **Change set** | A lista do que um deploy vai criar, alterar ou apagar, antes de aplicar |
| **CI/CD** | Compilar e testar a cada mudança (CI) e publicar automaticamente o que foi aceito (CD) |
| **CloudFormation** | Serviço da AWS que cria e mantém recursos a partir de um template |
| **CloudTrail** | Registro de auditoria: quem fez o quê na conta |
| **CloudWatch** | Logs, métricas e alarmes |
| **Cold start** | Demora da primeira execução de uma Lambda, enquanto o ambiente é preparado |
| **`CodeUri`** | No template, o pacote (`.zip`) que vai para cada Lambda |
| **DLQ** | *Dead-letter queue*: fila para onde vão as mensagens que falharam várias vezes |
| **Drift** | Diferença entre o template e o que está na AWS, causada por mudança manual no console |
| **DynamoDB** | Banco de dados NoSQL gerenciado, chave-valor |
| **Evento** | O que dispara uma Lambda: uma requisição HTTP, uma mensagem na fila, um horário |
| **FaaS** | *Function as a Service*: rodar funções sem gerenciar servidor (a Lambda) |
| **Handler** | O ponto de entrada de uma Lambda. Aqui é o do Quarkus, que chama a nossa classe `@Named` |
| **IAM** | Serviço de identidades e permissões da AWS |
| **IaC** | Infraestrutura como código: a infraestrutura descrita num arquivo versionado |
| **ID lógico / ID físico** | Nome do recurso no template / nome real dele na AWS |
| **Idempotente** | Que pode ser repetido sem efeito extra: rodar o deploy duas vezes dá o mesmo resultado |
| **Job / Step** | Bloco de trabalho do pipeline / cada passo dentro dele |
| **Lambda** | Serviço que executa o nosso código quando um evento acontece |
| **Módulo (Maven)** | Uma pasta com `pom.xml` próprio. Aqui, um por Lambda, mais o `shared` |
| **OIDC** | Login sem senha por token temporário. Bloqueado no nosso projeto pela SCP |
| **`pk` / `sk`** | Chave de partição / chave de ordenação de um item no DynamoDB |
| **Policy** | Documento JSON com o que uma identidade pode ou não fazer |
| **Privilégio mínimo** | Dar só a permissão necessária |
| **Projeto (AWS)** | Na experiência nova da AWS, uma conta separada, gerenciada pela AWS, com membros convidados |
| **Provisionada (capacidade)** | Leituras e escritas por segundo reservadas no DynamoDB. Necessária para a cota gratuita |
| **Quarkus** | Framework Java usado nas Lambdas (inicia rápido, injeção de dependências) |
| **Query / Scan** | Busca por chave (eficiente) / leitura da tabela inteira (evitar) |
| **Região** | Onde os recursos rodam. A nossa é `us-east-2` (Ohio) |
| **Role** | Identidade IAM sem senha, usada temporariamente por um serviço (ex.: a Lambda) |
| **Rollback** | O CloudFormation desfazendo um deploy que falhou |
| **S3** | Armazenamento de arquivos. Aqui, só o bucket do SAM com os pacotes |
| **SAM** | Extensão do CloudFormation para serverless, mais a CLI `sam` |
| **SCP** | Política que a AWS aplica no projeto, acima das nossas permissões, e que nós não podemos mudar |
| **Secret (GitHub)** | Valor criptografado no repositório, usado pelo pipeline e escondido nos logs |
| **Serverless** | Rodar aplicações sem gerenciar servidores |
| **SQS** | Serviço de filas de mensagens |
| **Stack (pilha)** | O conjunto de recursos criado a partir de um template, gerenciado como uma unidade |
| **Stage** | Versão publicada de uma API (`prod`), parte da URL |
| **Template** | O arquivo que descreve a infraestrutura (`template.yaml`) |
| **Usage plan** | Limites de uso por API key (throttling e cota) |
| **Visibility timeout** | Tempo em que uma mensagem em processamento fica escondida dos outros consumidores |
| **Workflow** | O arquivo do pipeline no GitHub Actions (`.github/workflows/ci-cd.yml`) |
| **X-Ray** | Rastreamento do caminho de uma requisição entre os serviços |

---

[← Anterior: perguntas frequentes](07-perguntas-frequentes.md) · [Índice](README.md)
