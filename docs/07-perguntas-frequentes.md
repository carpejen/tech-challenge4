# 7. Perguntas frequentes

[← Anterior: o caminho de uma avaliação](06-fluxo-de-uma-avaliacao.md) · [Índice](README.md) · [Próximo: glossário →](08-glossario.md)

## Deploy

### Posso fazer deploy mais de uma vez?
**Pode, quantas vezes quiser.** O deploy é *idempotente*: rodar de novo sem mudar nada não faz nada.
O CloudFormation compara o `template.yaml` com o que já existe e, sem diferença, termina com
"No changes to deploy". O pipeline está configurado para isso não ser erro
(`fail_on_empty_changeset = false`).

### Cada deploy cria recursos novos (outra Lambda, outra tabela, outra fila)?
**Não.** Cada recurso tem um **ID lógico** no template (`TabelaAvaliacoes`, `FilaCriticas`,
`ReceberAvaliacaoFunction`...). Enquanto o ID lógico continuar no template, o CloudFormation
**reaproveita o mesmo recurso** e só altera o que mudou. Um recurso novo só aparece se alguém
**adicionar** um bloco novo ao template.

### Então quando o deploy cria, altera ou apaga alguma coisa?

| Mudança no template | Efeito na AWS |
|---|---|
| Nada mudou | Nada |
| Código da Lambda mudou | A **mesma** Lambda recebe o pacote novo |
| Uma propriedade mudou (memória, timeout, retenção de logs...) | O **mesmo** recurso é atualizado |
| Um bloco novo (ex.: a Lambda `notificar-urgencia`) | O recurso é **criado** |
| Um bloco foi **removido** | O recurso é **apagado** da AWS |

### Os dados do DynamoDB somem a cada deploy?
**Não.** Atualizar a stack não mexe nos itens da tabela. Só há dois casos em que os dados se perdem:

1. Alguém **remove** a tabela do template, e o CloudFormation apaga a tabela.
2. Alguém muda algo que obriga a **recriar** a tabela, como `TableName` ou a chave (`KeySchema`).
   O CloudFormation cria uma tabela nova e apaga a antiga.

Antes de mexer na tabela, leia o *change set* no log do deploy. Se aparecer `Replacement: True`,
a tabela vai ser recriada.

### A URL da API ou a API key mudam a cada deploy?
**Não.** Elas só mudam se a stack for **apagada e criada de novo**.

### O que acontece se o deploy falhar no meio?
O CloudFormation faz **rollback**: desfaz o que tinha começado e a versão anterior continua no ar
(status `UPDATE_ROLLBACK_COMPLETE`). Corrija, abra um novo PR e faça o merge.

A exceção é a **primeira** criação: se ela falhar, a stack fica em `ROLLBACK_COMPLETE` e precisa ser
**apagada** antes de tentar de novo.

### E se dois PRs forem mergeados quase juntos?
Os deploys vão para a **fila**. O job `deploy` usa `concurrency: deploy-aws`, então um espera o outro
terminar. O CloudFormation também não deixa duas atualizações simultâneas na mesma stack.

### Abrir um PR faz deploy?
**Não.** No PR só roda o job `build` (compila e testa). O deploy acontece **só depois do merge na
`main`**.

### Mudar a versão no `pom.xml` faz deploy?
**Não.** O que dispara o deploy é o **merge na `main`**, não a versão. E cuidado: os módulos
(`shared`, `receber-avaliacao`) apontam para a versão do POM pai (`1.0.0-SNAPSHOT`). Se a versão do
pai mudar sozinha, os módulos deixam de encontrar o pai e o build quebra.

### Preciso rodar algo na minha máquina para fazer deploy?
**Não.** O deploy roda no GitHub Actions. Na sua máquina basta `mvn verify` para rodar os testes
antes de abrir o PR.

### Como desfaço uma mudança que quebrou alguma coisa?
`git revert <commit>` numa branch → PR → merge. O pipeline publica de novo a versão anterior.

### Posso ajustar alguma coisa direto no console da AWS?
**Para olhar, sim. Para mudar algo que está no template, não.** O próximo deploy pode desfazer a
mudança, e o repositório deixa de representar o que está rodando. O CloudFormation chama isso de
*drift* (desvio). Toda mudança passa pelo `template.yaml`.

## Custo

### Quanto custa cada deploy ou cada teste?
**Nenhum dinheiro.** O projeto está no plano gratuito, que não cobra cartão. Lambda, DynamoDB
provisionado, SQS e CloudWatch básico ficam dentro das cotas sempre gratuitas. O API Gateway e o
bucket do SAM consomem uma fração de centavo dos créditos.

### A Lambda fica ligada (e gastando) o tempo todo?
**Não.** Ela só executa quando chega uma requisição. Parada, não consome nada. O "preço" disso é o
cold start da primeira chamada.

## Estrutura

### Por que uma pasta (módulo) por Lambda, mas uma stack só?
Pastas separadas deixam a **responsabilidade única** visível e fazem cada Lambda carregar só as
suas dependências. Uma stack só mantém **um deploy, um pipeline e um lugar** para ver tudo.

### Quero criar outro recurso (fila, tópico, alarme...): onde coloco?
No `template.yaml`, dentro de `Resources:`, e nunca pelo console. O passo a passo com exemplo está em
[capítulo 3](03-cloudformation-e-sam.md#quero-criar-outro-recurso-onde-coloco).

### Qual configuração cria a Lambda?
O bloco `ReceberAvaliacaoFunction` do `template.yaml`, mais os valores comuns em `Globals`. Está
comentado linha a linha no [capítulo 3](03-cloudformation-e-sam.md#o-bloco-que-cria-a-lambda-linha-a-linha).

### Como crio a próxima Lambda?
Siga o `README.md` da pasta dela (`notificar-urgencia/` ou `gerar-relatorio-semanal/`):
1. Crie o módulo Maven.
2. Descomente o `<module>` no POM pai.
3. Adicione a função no `template.yaml`, com o `CodeUri` dela.
4. Abra o PR.

### Por que o deploy usa access key e não OIDC?
Porque o projeto da nova experiência AWS bloqueia a criação do provedor OIDC com uma política que a
AWS gerencia. Veja [capítulo 5](05-deploy.md#credenciais-por-que-um-usuário-iam-e-não-oidc).

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Job `deploy` com aviso "secrets não configurados" | Faltam `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` no GitHub |
| `InvalidClientTokenId` / `SignatureDoesNotMatch` | Secret copiado errado |
| `... is not authorized to perform ...` no deploy | Falta essa permissão no usuário de deploy: adicione em `infra/github-deploy-user.yaml` e atualize a pilha `github-deploy-user` |
| Stack em `ROLLBACK_COMPLETE` | Leia o primeiro evento vermelho (aba **Eventos**) e apague a stack antes de tentar de novo |
| `Caller does not have permissions to create a Service Linked Role` no stage do API Gateway | O usuário de deploy precisava de `iam:CreateServiceLinkedRole` para a role `AWSServiceRoleForAPIGateway` (já corrigido em `infra/github-deploy-user.yaml`) |
| `403` na chamada da API | Faltou o header `x-api-key`, ou o valor está errado |
| `429` na chamada da API | Limite do plano de uso (5 req/s, pico de 10) |
| `500` na chamada da API | Veja o log da Lambda no CloudWatch |
| Primeira chamada demora segundos | Cold start do Java. As seguintes são rápidas |

---

[← Anterior: o caminho de uma avaliação](06-fluxo-de-uma-avaliacao.md) · [Índice](README.md) · [Próximo: glossário →](08-glossario.md)
