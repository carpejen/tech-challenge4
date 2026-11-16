// Conteúdo da apresentação: textos dos slides, links e roteiros das animações.
// Para ajustar um texto, edite aqui. O visual fica em estilo.css.

export const PROJETO = {
  regiao: 'us-east-2',
  stack: 'feedback-serverless',
  apiId: 't535ci5ig1', // trocado pelo host da collection Postman quando a página carrega
  repo: 'https://github.com/carpejen/tech-challenge4',
};

/** Links do console da AWS e do GitHub, montados a partir dos nomes do template.yaml. */
export function montarLinks(apiId = PROJETO.apiId) {
  const { regiao: r, stack: s, repo } = PROJETO;
  const c = `https://${r}.console.aws.amazon.com`;
  const cw = `${c}/cloudwatch/home?region=${r}`;
  const funcao = (nome) => `${c}/lambda/home?region=${r}#/functions/${s}-${nome}`;
  const logs = (nome) => `${cw}#logsV2:log-groups/log-group/$252Faws$252Flambda$252F${s}-${nome}`;

  return {
    stack: `${c}/cloudformation/home?region=${r}#/stacks?filteringText=${s}&filteringStatus=active&viewNested=true`,
    fnReceber: funcao('receber-avaliacao'),
    fnNotificar: funcao('notificar-urgencia'),
    fnRelatorio: funcao('gerar-relatorio-semanal'),
    logsReceber: logs('receber-avaliacao'),
    logsNotificar: logs('notificar-urgencia'),
    logsRelatorio: logs('gerar-relatorio-semanal'),
    api: `${c}/apigateway/main/apis/${apiId}/resources?api=${apiId}&region=${r}`,
    planos: `${c}/apigateway/main/usage-plans?region=${r}`,
    chaves: `${c}/apigateway/main/api-keys?region=${r}`,
    itens: `${c}/dynamodbv2/home?region=${r}#item-explorer?operation=SCAN&table=${s}-avaliacoes`,
    filas: `${c}/sqs/v3/home?region=${r}#/queues`,
    ses: `${c}/ses/home?region=${r}#/identities`,
    agenda: `${c}/scheduler/home?region=${r}#/schedules/default/GerarRelatorioSemanalFunctionRelatorioSemanal`,
    dashboard: `${cw}#dashboards/dashboard/${s}-monitoramento`,
    alarmes: `${cw}#alarmsV2:`,
    xray: `${cw}#xray:service-map/map`,
    parametros: `${c}/systems-manager/parameters/?region=${r}&tab=Table`,
    usuarioDeploy: `https://us-east-1.console.aws.amazon.com/iam/home#/users/details/github-deploy-${s}`,
    repo,
    actions: `${repo}/actions`,
    workflow: `${repo}/blob/main/.github/workflows/ci-cd.yml`,
    template: `${repo}/blob/main/template.yaml`,
    docs: `${repo}/tree/main/docs`,
    collection: `${repo}/blob/main/docs/Tech_Challenge_4_Postman_Collection.json`,
  };
}

/** Link externo; o endereço é preenchido depois por preencherLinks(). */
export const link = (chave, rotulo) =>
  `<a class="link-ext" data-link="${chave}" target="_blank" rel="noopener">${rotulo}<i aria-hidden="true">→</i></a>`;

/** Coloca o endereço em todo <a data-link> dentro de `raiz`. */
export function preencherLinks(raiz = document) {
  const links = montarLinks();
  for (const a of raiz.querySelectorAll('a[data-link]')) {
    if (links[a.dataset.link]) a.href = links[a.dataset.link];
  }
}

/**
 * Logo da FIAP, redesenhada em vetor a partir da imagem enviada pelo grupo.
 * Para usar o arquivo oficial, troque este SVG por <img class="logo-fiap" src="...">.
 */
const LOGO_FIAP = `
  <svg class="logo-fiap" viewBox="134 119 584 165" role="img" aria-label="FIAP">
    <defs>
      <clipPath id="logo-fiap-a">
        <rect x="330" y="100" width="117" height="178"/>
        <rect x="447" y="100" width="120" height="112"/>
        <rect x="447" y="237" width="120" height="41"/>
      </clipPath>
    </defs>
    <g fill="none" stroke="currentColor" stroke-width="12" stroke-miterlimit="10">
      <path d="M146,278 V131 H270"/>
      <path d="M185,203 H240"/>
      <path d="M311,125 V278"/>
      <path clip-path="url(#logo-fiap-a)" d="M342,293 L447,136 L552,293"/>
      <path d="M571,278 V131 H666 a40.5,40.5 0 0 1 0,81 H620"/>
    </g>
  </svg>`;

const INTEGRANTES = [
  ['Felipe Ulson Sora', 'RM370766'],
  ['Gabriel Alberto Ferreira Pinto', 'RM374005'],
  ['Jeniffer da Nobrega Bandeira', 'RM371936'],
];

const REQUISITOS = [
  ['Requisito', 'Nuvem com segurança e governança de acesso', 'AWS · IAM de menor privilégio · API key · criptografia'],
  ['Requisito', 'Componentes de suporte configurados', 'DynamoDB · SQS · SES · Parameter Store'],
  ['Requisito', 'Deploy automatizado', 'GitHub Actions + AWS SAM a cada push na main'],
  ['Requisito', 'Aplicação monitorada', 'CloudWatch: logs, 5 alarmes e dashboard · X-Ray'],
  ['Requisito', 'Notificação automática de problemas críticos', 'SQS → Lambda → e-mail pelo SES'],
  ['Requisito', 'Relatório semanal com a média das avaliações', 'EventBridge Scheduler → Lambda → SES'],
  ['Regra', 'Implementar serverless', '3 funções AWS Lambda em Java 21 + Quarkus'],
  ['Regra', 'Rodar em ambiente cloud', 'Uma stack SAM/CloudFormation em us-east-2'],
  ['Regra', 'Mínimo de 2 funções, responsabilidade única', 'Uma função por caso de uso'],
];

const ADERENCIA = [
  ['Modelo de cloud explicado', 'FaaS com AWS Lambda e serviços gerenciados'],
  ['Aplicação funcionando', 'API no ar · demo ao vivo com a collection'],
  ['Código com documentação', 'README + pasta docs/ com 8 capítulos'],
  ['Arquitetura da solução', 'Diagrama, fluxo e modelo de dados'],
  ['Instruções de deploy', 'Pipeline automático e deploy manual com SAM'],
  ['Monitoramento', 'Logs, 5 alarmes, dashboard e X-Ray'],
  ['Documentação das funções', 'Gatilho, responsabilidade e permissões de cada uma'],
  ['Segurança', 'HTTPS, API key, IAM mínimo, criptografia, Secrets'],
];

/** Passos do pipeline (slide de deploy). */
export const PIPELINE = {
  pr: {
    passos: [
      ['checkout', 'Baixa o código do pull request'],
      ['Java 21', 'Temurin, com cache do Maven'],
      ['mvn -B verify', 'Compila e testa todos os módulos'],
      ['function.zip', 'Guarda um pacote por Lambda'],
    ],
    fim: 'Pull request só valida. Nenhum deploy acontece aqui.',
  },
  main: {
    passos: [
      ['Build e testes', 'O mesmo job do pull request'],
      ['Artifacts', 'Baixa os function.zip gerados'],
      ['Credenciais AWS', 'Secrets do usuário IAM de deploy'],
      ['sam deploy', 'Lê template.yaml e samconfig.toml'],
      ['CloudFormation', 'Cria ou atualiza a stack'],
    ],
    fim: 'Stack feedback-serverless atualizada, sem passo manual.',
  },
};

const lista = (itens) => `<ul class="mais">${itens.map((i) => `<li class="a">${i}</li>`).join('')}</ul>`;

export const SLIDES = [
  // ---------------------------------------------------------------- 01
  {
    id: 'capa',
    secao: 'Abertura',
    nome: 'Capa',
    foco: [0.78, 0.38],
    semCabecalho: true,
    corpo: `
      <div class="capa">
        <div class="capa-texto">
          <div class="capa-marca a">${LOGO_FIAP}<span>Pós-Tech <i>·</i> Arquitetura e Desenvolvimento Java</span></div>
          <h1 class="a">Plataforma<br>de feedback<br><span class="contorno">serverless</span></h1>
          <p class="lead barra a">Tech Challenge — Fase 4<br>Cloud Computing, Serverless e Deploy de Aplicações</p>
          <ul class="integrantes">
            ${INTEGRANTES.map(([nome, rm]) => `<li class="a"><span>${nome}</span><b>${rm}</b></li>`).join('')}
          </ul>
        </div>
        <div class="capa-arte a" aria-hidden="true">
          <div class="cubo c1"><i></i><i></i><i></i><i></i><i></i><i></i></div>
          <div class="cubo c2"><i></i><i></i><i></i><i></i><i></i><i></i></div>
          <div class="cubo c3"><i></i><i></i><i></i><i></i><i></i><i></i></div>
        </div>
      </div>
      <p class="dica a">→ avança <span>·</span> ← volta <span>·</span> O índice <span>·</span> F tela cheia</p>`,
  },

  // ---------------------------------------------------------------- 02
  {
    id: 'desafio',
    secao: 'O desafio',
    nome: 'Problema e objetivo',
    foco: [0.2, 0.6],
    titulo: 'Ouvir o aluno. <em>Agir rápido.</em>',
    corpo: `
      <div class="duas">
        <div class="col">
          <p class="lead barra a">Para garantir a qualidade dos cursos on-line, os estudantes precisam avaliar as aulas — e os administradores precisam enxergar a satisfação sem demora.</p>
          <p class="lead barra a">O sistema recebe os feedbacks, avisa quando algo é crítico e entrega um relatório periódico para análise.</p>
          <p class="rodape a">Entrega: repositório aberto + vídeo de demonstração</p>
        </div>
        <ol class="verbos">
          <li class="a"><span>01</span><div><b>Receber</b><p>avaliações das aulas por um endpoint HTTP</p></div></li>
          <li class="a"><span>02</span><div><b>Notificar</b><p>os administradores quando o item é crítico</p></div></li>
          <li class="a"><span>03</span><div><b>Relatar</b><p>toda semana, com média e totais</p></div></li>
        </ol>
      </div>`,
  },

  // ---------------------------------------------------------------- 03
  {
    id: 'requisitos',
    secao: 'Requisitos',
    nome: 'O que o enunciado pede',
    foco: [0.85, 0.2],
    titulo: 'O que foi pedido. <em>O que entregamos.</em>',
    corpo: `
      <ul class="req">
        ${REQUISITOS.map(([tipo, pedido, entrega]) => `
          <li class="a">
            <svg class="visto" viewBox="0 0 32 32" aria-hidden="true"><circle cx="16" cy="16" r="14"/><path d="M9.5 16.5l4.5 4.5 8.5-9.5"/></svg>
            <div><small>${tipo}</small><b>${pedido}</b><span>${entrega}</span></div>
          </li>`).join('')}
      </ul>`,
  },

  // ---------------------------------------------------------------- 04
  {
    id: 'modelo',
    secao: 'Modelo de cloud',
    nome: 'FaaS',
    foco: [0.7, 0.55],
    titulo: 'FaaS — <em>function as a service</em>',
    corpo: `
      <div class="duas">
        <div class="col">
          <p class="lead barra a">Nenhum servidor ligado à espera. Cada função nasce de um evento, executa e termina.</p>
          ${lista([
            'Execução sob demanda',
            'Escala gerenciada pela AWS',
            'Baixo acoplamento entre as partes',
            'Infraestrutura reproduzível, descrita em código',
          ])}
        </div>
        <ul class="gatilhos">
          <li class="a"><div class="evento"><small>HTTP</small><b>POST /avaliacao</b></div><i class="fio"></i><div class="alvo"><small>Lambda</small><b>receber-avaliacao</b></div></li>
          <li class="a"><div class="evento"><small>Fila</small><b>mensagem no SQS</b></div><i class="fio"></i><div class="alvo"><small>Lambda</small><b>notificar-urgencia</b></div></li>
          <li class="a"><div class="evento"><small>Agenda</small><b>segunda, 08:00</b></div><i class="fio"></i><div class="alvo"><small>Lambda</small><b>gerar-relatorio-semanal</b></div></li>
        </ul>
      </div>
      <div class="faixa a">
        <span>Tudo em uma única stack AWS SAM / CloudFormation: <code>feedback-serverless</code> em <code>us-east-2</code></span>
        ${link('stack', 'Abrir a stack')}
      </div>`,
  },

  // ---------------------------------------------------------------- 05
  {
    id: 'arquitetura',
    secao: 'Arquitetura',
    nome: 'Arquitetura',
    foco: [0.5, 0.5],
    titulo: 'O caminho de uma <em>avaliação</em>',
    corpo: `<div class="diag a" data-diagrama></div>`,
  },

  // ---------------------------------------------------------------- 06
  {
    id: 'funcoes',
    secao: 'Funções serverless',
    nome: 'As 3 funções',
    foco: [0.5, 0.25],
    titulo: 'Três funções. <em>Uma responsabilidade cada.</em>',
    corpo: `
      <div class="funcoes">
        <article class="cartao fn a">
          <small>Lambda 01</small>
          <h3>receber-avaliacao</h3>
          <dl>
            <dt>Gatilho</dt><dd>API Gateway · <code>POST /avaliacao</code></dd>
            <dt>Responsabilidade</dt><dd>Validar, classificar a urgência, gravar e encaminhar as críticas</dd>
            <dt>Permissões IAM</dt><dd><code>dynamodb:PutItem</code> <code>sqs:SendMessage</code></dd>
          </dl>
          <footer>${link('fnReceber', 'Função')}${link('logsReceber', 'Logs')}</footer>
        </article>
        <article class="cartao fn a">
          <small>Lambda 02</small>
          <h3>notificar-urgencia</h3>
          <dl>
            <dt>Gatilho</dt><dd>Fila SQS · 1 mensagem por vez</dd>
            <dt>Responsabilidade</dt><dd>Enviar ao administrador o alerta de uma avaliação crítica</dd>
            <dt>Permissões IAM</dt><dd><code>ses:SendEmail</code></dd>
          </dl>
          <footer>${link('fnNotificar', 'Função')}${link('logsNotificar', 'Logs')}</footer>
        </article>
        <article class="cartao fn a">
          <small>Lambda 03</small>
          <h3>gerar-relatorio-semanal</h3>
          <dl>
            <dt>Gatilho</dt><dd>EventBridge Scheduler · <code>cron(0 8 ? * MON *)</code></dd>
            <dt>Responsabilidade</dt><dd>Consolidar os 7 dias anteriores e enviar o relatório</dd>
            <dt>Permissões IAM</dt><dd><code>dynamodb:Query</code> <code>ses:SendEmail</code></dd>
          </dl>
          <footer>${link('fnRelatorio', 'Função')}${link('logsRelatorio', 'Logs')}</footer>
        </article>
      </div>
      <p class="rodape a">Em comum: Java 21 · Quarkus 3 · 512 MB · timeout de 20 s · X-Ray ativo · log group próprio · um <code>function.zip</code> por módulo Maven</p>`,
  },

  // ---------------------------------------------------------------- 07
  {
    id: 'urgencia',
    secao: 'Regra de negócio',
    nome: 'Regra de urgência',
    foco: [0.3, 0.45],
    titulo: 'Da nota à <em>urgência</em>',
    corpo: `
      <div class="urg a" data-urgencia>
        <div class="urg-esq">
          <span class="rotulo">Escolha uma nota</span>
          <div class="urg-notas" data-notas role="radiogroup" aria-label="Nota da avaliação"></div>
          <div class="urg-faixas" aria-hidden="true">
            <span class="critica">0 a 4<b>Crítica</b></span>
            <span class="media">5 a 7<b>Média</b></span>
            <span class="baixa">8 a 10<b>Baixa</b></span>
          </div>
          <span class="rotulo">Caminho que a avaliação percorre</span>
          <div class="urg-caminho" data-caminho-urg>
            <span>API Gateway</span><span>Lambda</span><span>DynamoDB</span>
            <span data-so-critica>SQS</span><span data-so-critica>Lambda</span><span data-so-critica>SES</span>
          </div>
          <p class="rodape">O enunciado não define a regra — esta foi a decisão do grupo. A urgência é calculada uma única vez, no recebimento, e gravada junto com a avaliação.</p>
        </div>
        <div class="urg-dir cartao">
          <span class="rotulo">Resultado</span>
          <strong data-resultado>CRITICA</strong>
          <ul class="urg-efeitos">
            <li data-efeito="grava">Grava no DynamoDB</li>
            <li data-efeito="relatorio">Entra no relatório semanal</li>
            <li data-efeito="fila">Publica na fila SQS</li>
            <li data-efeito="email">E-mail imediato ao administrador</li>
          </ul>
        </div>
      </div>`,
  },

  // ---------------------------------------------------------------- 08
  {
    id: 'dados',
    secao: 'Dados',
    nome: 'Modelo de dados',
    foco: [0.75, 0.6],
    titulo: 'DynamoDB: <em>um dia por partição</em>',
    corpo: `
      <div class="duas dados">
        <div class="col">
          <div class="chaves a">
            <div><small>pk</small><code>DIA#2026-11-03</code><span>o dia, no horário de Brasília</span></div>
            <div><small>sk</small><code>2026-11-03T19:14:23Z#9b83f1d0…</code><span>instante + id: ordenado e único</span></div>
          </div>
          <pre class="codigo a"><code>{
  <i>"id"</i>:        <b>"9b83f1d0-…"</b>,
  <i>"descricao"</i>: <b>"Aula com áudio ruim"</b>,
  <i>"nota"</i>:      <u>2</u>,
  <i>"urgencia"</i>:  <b>"CRITICA"</b>,
  <i>"dataEnvio"</i>: <b>"2026-11-03T19:14:23Z"</b>
}</code></pre>
          <div class="a">${link('itens', 'Ver os itens da tabela')}</div>
        </div>
        <div class="col">
          <div class="semana a" data-semana>
            <span class="rotulo">Relatório semanal: 7 dias = 7 Queries</span>
            <ol>${['seg', 'ter', 'qua', 'qui', 'sex', 'sáb', 'dom'].map((d) => `<li><b>${d}</b><i>Query</i></li>`).join('')}</ol>
            <p><s>Scan</s> na tabela inteira não é usado.</p>
          </div>
          ${lista([
            'Capacidade provisionada: 5 RCU / 5 WCU, dentro da cota gratuita',
            'Criptografia em repouso com chave gerenciada pela AWS',
            'Feedback anônimo: só descrição e nota são guardadas',
          ])}
        </div>
      </div>`,
  },

  // ---------------------------------------------------------------- 09
  {
    id: 'seguranca',
    secao: 'Segurança e governança',
    nome: 'Segurança',
    foco: [0.15, 0.3],
    titulo: 'Segurança <em>em camadas</em>',
    corpo: `
      <div class="grade3">
        <article class="cartao a"><small>01 · Transporte</small><h3>HTTPS</h3><p>Toda chamada entra por TLS no API Gateway.</p></article>
        <article class="cartao a"><small>02 · Acesso</small><h3>API key + plano de uso</h3><p>Sem <code>x-api-key</code>, resposta 403. Limite de 5 req/s, rajada de 10 e 10 mil chamadas por mês.</p></article>
        <article class="cartao a"><small>03 · Permissões</small><h3>IAM de menor privilégio</h3><p>Cada Lambda recebe só as ações de que precisa, no recurso exato.</p></article>
        <article class="cartao a"><small>04 · Dados</small><h3>Criptografia em repouso</h3><p>DynamoDB com chave gerenciada pela AWS. Filas SQS com SSE.</p></article>
        <article class="cartao a"><small>05 · Configuração</small><h3>Fora do código</h3><p>Os e-mails ficam no Parameter Store: <code>/feedback/email-remetente</code> e <code>/feedback/email-admin</code>.</p></article>
        <article class="cartao a"><small>06 · Pipeline</small><h3>Segredos no GitHub</h3><p>A chave do usuário de deploy vive só em Secrets. Nenhuma credencial no repositório.</p></article>
      </div>
      <div class="faixa a">
        <span>Governança: toda a infraestrutura está em <code>template.yaml</code> e só muda por pull request.</span>
        ${link('planos', 'Plano de uso')}${link('parametros', 'Parâmetros')}${link('usuarioDeploy', 'Usuário de deploy')}
      </div>`,
  },

  // ---------------------------------------------------------------- 10
  {
    id: 'demo',
    secao: 'Demo ao vivo',
    nome: 'Chamando a API',
    foco: [0.5, 0.15],
    titulo: 'A API, <em>ao vivo</em>',
    corpo: `<div class="con a" data-console></div>`,
  },

  // ---------------------------------------------------------------- 11
  {
    id: 'notificacao',
    secao: 'Notificação crítica',
    nome: 'Notificação crítica',
    foco: [0.8, 0.5],
    titulo: 'Crítica? <em>O administrador sabe na hora.</em>',
    corpo: `
      <div class="duas">
        <ol class="trilha">
          <li class="a"><b>receber-avaliacao</b> publica a mensagem na fila</li>
          <li class="a"><b>SQS</b> entrega uma mensagem por vez à função</li>
          <li class="a"><b>notificar-urgencia</b> monta o alerta e envia pelo SES</li>
          <li class="a">Falhou? A mensagem volta à fila: até <b>3 tentativas</b></li>
          <li class="a">Na terceira falha vai para a <b>DLQ</b> e um alarme dispara</li>
        </ol>
        <div class="col">
          <figure class="email a">
            <figcaption><small>Assunto</small>[Feedback] Avaliação CRITICA recebida (nota 2)<em>exemplo</em></figcaption>
            <pre>Uma avaliação crítica foi recebida e precisa de atenção.

<mark>Descrição</mark>: A aula apresentou problemas graves no conteudo
Nota: 2
<mark>Urgência</mark>: CRITICA
<mark>Data de envio</mark>: 03/11/2026 16:14:23 (horário de Brasília)
ID: 9b83f1d0-…</pre>
          </figure>
          <p class="rodape a"><mark>Em destaque</mark>, os três campos que o enunciado pede no aviso de urgência.</p>
          <div class="links a">${link('filas', 'Filas SQS')}${link('logsNotificar', 'Logs da função')}${link('ses', 'SES')}</div>
        </div>
      </div>`,
  },

  // ---------------------------------------------------------------- 12
  {
    id: 'relatorio',
    secao: 'Relatório semanal',
    nome: 'Relatório semanal',
    foco: [0.25, 0.3],
    titulo: 'Toda segunda, <em>08:00</em>',
    corpo: `
      <div class="duas">
        <div class="col">
          <div class="agenda a" aria-hidden="true">
            ${['seg', 'ter', 'qua', 'qui', 'sex', 'sáb', 'dom'].map((d) => `<span>${d}</span>`).join('')}
            <span class="hoje">seg<b>08:00</b></span>
          </div>
          <p class="lead barra a"><code>cron(0 8 ? * MON *)</code> no fuso <code>America/Sao_Paulo</code>. A função consulta os sete dias completos anteriores — de segunda a domingo.</p>
          ${lista([
            'Até 2 novas tentativas se a invocação falhar',
            'Falha definitiva vai para uma DLQ própria, com alarme',
          ])}
          <div class="comando a">
            <span class="rotulo">Disparar agora, sem esperar a segunda</span>
            <code data-comando>aws lambda invoke --function-name feedback-serverless-gerar-relatorio-semanal --payload '{}' --cli-binary-format raw-in-base64-out --region us-east-2 resposta-relatorio.json</code>
            <button type="button" data-copiar>Copiar</button>
          </div>
          <div class="links a">${link('agenda', 'Agendamento')}${link('fnRelatorio', 'Função')}${link('logsRelatorio', 'Logs')}</div>
        </div>
        <figure class="email alto a">
          <figcaption><small>Assunto</small>[Feedback] Relatório semanal 02/11/2026 a 08/11/2026<em>exemplo</em></figcaption>
          <pre>Relatório semanal de feedbacks

Período: 02/11/2026 a 08/11/2026
Total de avaliações: 4
<mark>Média das notas</mark>: 6.50

<mark>Quantidade por dia</mark>:
- 02/11/2026: 1
- 03/11/2026: 2
- 04/11/2026: 1
- …

<mark>Quantidade por urgência</mark>:
- CRITICA: 1
- MEDIA: 1
- BAIXA: 2

Avaliações:
- <mark>Data de envio</mark>: 03/11/2026 16:14:23
  <mark>Urgência</mark>: CRITICA
  Nota: 2
  <mark>Descrição</mark>: A aula apresentou problemas…</pre>
        </figure>
      </div>`,
  },

  // ---------------------------------------------------------------- 13
  {
    id: 'monitoramento',
    secao: 'Monitoramento',
    nome: 'Monitoramento',
    foco: [0.85, 0.3],
    titulo: 'Nada falha <em>em silêncio</em>',
    corpo: `
      <div class="duas monit">
        <div class="col">
          <span class="rotulo a">5 alarmes no CloudWatch</span>
          <ul class="alarmes">
            <li class="a"><i></i><code>erro-receber-avaliacao</code><span>erro na Lambda</span></li>
            <li class="a"><i></i><code>erro-notificar-urgencia</code><span>erro na Lambda</span></li>
            <li class="a"><i></i><code>erro-gerar-relatorio-semanal</code><span>erro na Lambda</span></li>
            <li class="a"><i></i><code>mensagem-dlq-criticas</code><span>mensagem na DLQ</span></li>
            <li class="a"><i></i><code>mensagem-dlq-relatorio</code><span>mensagem na DLQ</span></li>
          </ul>
          <div class="links a">${link('alarmes', 'Alarmes')}${link('dashboard', 'Dashboard')}</div>
        </div>
        <div class="grade2">
          <article class="cartao a"><small>Dashboard</small><h3>4 painéis</h3><p>Invocações, erros, mensagens nas DLQs e throttling do DynamoDB.</p></article>
          <article class="cartao a"><small>Logs</small><h3>Um grupo por função</h3><p>Log group próprio para cada Lambda, com retenção de 7 dias.</p>${link('logsReceber', 'Abrir logs')}</article>
          <article class="cartao a"><small>X-Ray</small><h3>Rastreamento ativo</h3><p>Na API e nas três funções, do pedido HTTP até o banco.</p>${link('xray', 'Mapa de rastreamento')}</article>
          <article class="cartao a"><small>DLQs</small><h3>Duas filas de falha</h3><p>Guardam por 14 dias o que não pôde ser processado.</p>${link('filas', 'Abrir filas')}</article>
        </div>
      </div>`,
  },

  // ---------------------------------------------------------------- 14
  {
    id: 'deploy',
    secao: 'Deploy automatizado',
    nome: 'Deploy automatizado',
    foco: [0.4, 0.7],
    titulo: 'Do merge à nuvem, <em>sem passo manual</em>',
    corpo: `
      <div class="pipe a" data-pipeline>
        <div class="abas" role="tablist">
          <button type="button" class="botao" role="tab" data-pista="pr">Pull request</button>
          <button type="button" class="botao" role="tab" data-pista="main">Push na main</button>
        </div>
        <ol class="pista" data-passos></ol>
        <p class="pipe-fim" data-fim></p>
      </div>
      <div class="duas baixo">
        ${lista([
          'Infraestrutura em código: <code>template.yaml</code> + <code>samconfig.toml</code>',
          'Um deploy por vez: <code>concurrency: deploy-aws</code>',
          'Usuário IAM dedicado ao pipeline, com política restrita à stack',
        ])}
        <div class="col">
          <p class="rodape a">OIDC foi considerado, mas a política da conta bloqueia a criação do provedor. Por isso a chave do usuário de deploy fica nos Secrets do GitHub.</p>
          <div class="links a">${link('actions', 'Execuções')}${link('workflow', 'ci-cd.yml')}${link('template', 'template.yaml')}</div>
        </div>
      </div>`,
  },

  // ---------------------------------------------------------------- 15
  {
    id: 'fim',
    secao: 'Encerramento',
    nome: 'Aderência e encerramento',
    foco: [0.22, 0.4],
    semCabecalho: true,
    corpo: `
      <div class="duas fim">
        <div class="col">
          <div class="kicker a"><b>15</b>Encerramento</div>
          <h1 class="a menor">Obri<span class="contorno">gado</span></h1>
          <ul class="integrantes">
            ${INTEGRANTES.map(([nome, rm]) => `<li class="a"><span>${nome}</span><b>${rm}</b></li>`).join('')}
          </ul>
          <div class="links a">${link('repo', 'Repositório')}${link('docs', 'Documentação')}${link('collection', 'Collection Postman')}</div>
        </div>
        <div class="col">
          <span class="rotulo a">Critérios de avaliação</span>
          <table class="aderencia">
            ${ADERENCIA.map(([criterio, entrega]) => `<tr class="a"><th>${criterio}</th><td>${entrega}</td></tr>`).join('')}
          </table>
        </div>
      </div>`,
  },
];
