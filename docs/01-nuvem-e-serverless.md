# 1. Nuvem e serverless

[← Índice](README.md) · [Próximo: os serviços da AWS →](02-servicos-aws.md)

## O que é "nuvem"

Em vez de comprar um servidor e deixá-lo ligado numa sala, você **aluga recursos de computação de um
provedor** (aqui, a AWS) pela internet e paga pelo uso. O provedor cuida do hardware, da energia,
da rede e da segurança física. Você só configura o que precisa.

O enunciado pede para explicarmos o **modelo de cloud**. O nosso é:

| Aspecto | Nosso caso |
|---|---|
| Modelo de implantação | **Nuvem pública** (AWS) |
| Modelo de serviço | **Serverless**: funções (FaaS, *Function as a Service*) + serviços gerenciados |
| Cobrança | Por uso, dentro do **plano gratuito** da AWS |

## Conta, projeto e região

- **Conta AWS:** o "espaço" onde ficam os recursos, as permissões e a cobrança.
- **Projeto:** na experiência nova da AWS, cada projeto é **uma conta AWS separada**, criada e
  gerenciada pela AWS. O nosso se chama `tech-challenge-4-fiap-adj`. Quem é convidado para o projeto
  tem acesso de administrador aos recursos, mas não à parte financeira (cobrança, plano, limite de gastos).
- **Região:** o data center onde os recursos rodam. O nosso projeto está fixo em
  **`us-east-2` (Ohio)**. Tudo que criarmos fica lá, e por isso o pipeline e o `samconfig.toml`
  apontam para `us-east-2`.

> **Regras da AWS sobre o projeto (SCPs).** Na experiência nova, a AWS aplica *políticas de controle
> de serviço* (SCPs) que valem acima de qualquer permissão nossa. Uma delas impede criar
> "provedores de identidade" no IAM, e é por isso que o deploy usa um usuário com access key em vez
> de OIDC. O [capítulo 5](05-deploy.md#credenciais-por-que-um-usuário-iam-e-não-oidc) explica.

## O que é serverless

"Serverless" não significa que não existe servidor. Significa que **você não gerencia servidor
nenhum**:

| Servidor tradicional (ex.: EC2, VM) | Serverless (ex.: Lambda) |
|---|---|
| Fica ligado 24 horas, mesmo sem uso | Só roda quando chega uma requisição ou evento |
| Você instala SO, Java, atualizações | A AWS cuida de tudo; você entrega só o código |
| Você decide quantas máquinas ter | A AWS escala sozinha (uma, cem, mil execuções em paralelo) |
| Paga por hora ligada | Paga por execução (e o plano gratuito cobre muito) |

Uma aplicação serverless é feita de duas coisas:

1. **Funções (Lambda):** pedaços pequenos de código, cada um com uma responsabilidade, que rodam
   quando algo acontece (chega uma requisição HTTP, entra uma mensagem na fila, dá a hora agendada).
2. **Serviços gerenciados:** banco (DynamoDB), fila (SQS), API (API Gateway), logs (CloudWatch).
   Você usa, mas não administra.

## Arquitetura orientada a eventos

No nosso projeto **uma função não chama a outra**. Cada uma reage a um **evento**:

| Função | Evento que dispara |
|---|---|
| `receber-avaliacao` | Requisição HTTP `POST /avaliacao` |
| `notificar-urgencia` (próxima etapa) | Mensagem nova na fila SQS |
| `gerar-relatorio-semanal` (próxima etapa) | Agendamento: toda segunda às 08h |

Vantagem: se a notificação estiver fora do ar, o recebimento continua funcionando. A mensagem fica
esperando na fila. Isso é **baixo acoplamento**, e o enunciado avalia a "correta separação dos
serviços e responsabilidades".

## Cold start

Quando uma Lambda não roda há algum tempo, a AWS precisa **preparar um ambiente novo**: baixar o
pacote, iniciar a JVM e o Quarkus. Isso é o *cold start*, e a primeira chamada leva alguns segundos.
As seguintes reaproveitam o ambiente já quente e respondem em milissegundos.

Para a demonstração em vídeo: faça uma chamada de "aquecimento" antes de gravar.

## Custo

O projeto roda no **plano gratuito**: o cartão não é cobrado, e se os créditos acabassem a conta
seria encerrada, não cobrada. Além disso, quase tudo cabe nas cotas **sempre gratuitas** da AWS
(Lambda, DynamoDB provisionado, SQS, CloudWatch básico). O [capítulo 2](02-servicos-aws.md) mostra a
cota de cada serviço.

---

[← Índice](README.md) · [Próximo: os serviços da AWS →](02-servicos-aws.md)
