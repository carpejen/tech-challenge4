# Lambda `gerar-relatorio-semanal`

| | |
|---|---|
| **Gatilho** | EventBridge Scheduler: segunda-feira às 08h, fuso `America/Sao_Paulo` |
| **Responsabilidade** | Consolidar os 7 dias completos anteriores e enviar o relatório ao administrador |
| **Leitura** | DynamoDB: 7 `Query`, uma por dia, usando `TabelaAvaliacoes.chaveParticao(LocalDate)` |
| **Saída** | E-mail via SES |

## Período

Como a execução normal ocorre na segunda-feira às 08h, o relatório consolida a semana anterior completa:
segunda-feira até domingo. A regra no código é "os sete dias completos anteriores ao dia da execução",
o que também torna a função previsível quando invocada manualmente.

## Conteúdo do relatório

- total de avaliações;
- média das notas;
- quantidade por dia;
- quantidade por urgência (`CRITICA`, `MEDIA`, `BAIXA`);
- lista das avaliações com descrição, nota, urgência e data de envio em horário de Brasília.

Mesmo sem avaliações, um e-mail é enviado informando total zero e média `N/A`.

## Estrutura

```text
br/fiap/feedback/relatorio/
├── config/      RelogioProducer
├── function/    GerarRelatorioSemanalHandler
├── service/     RelatorioSemanalService
├── model/       RelatorioSemanal
├── repository/  AvaliacaoConsultaRepository + DynamoDbAvaliacaoConsultaRepository
└── email/       EnviadorRelatorio + SesEnviadorRelatorio
```

## Infraestrutura

O `template.yaml` configura:

- `CodeUri: gerar-relatorio-semanal/target/function.zip`;
- `ScheduleV2` toda segunda às 08h em `America/Sao_Paulo`;
- retry do Scheduler + DLQ SQS compartilhada com a configuração assíncrona da Lambda;
- permissão mínima `dynamodb:Query` na tabela;
- permissão `ses:SendEmail` nas identidades SES da conta/região;
- grupo de logs com retenção de 7 dias.
