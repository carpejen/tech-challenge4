// Diagrama da arquitetura (slide 05): nós clicáveis e fluxos animados.
// As posições estão em unidades do viewBox do SVG.

import { link, preencherLinks } from './conteudo.js';

const ALTURA_NO = 100;

/** [x, y, largura, tipo, nome, subtítulo, texto do detalhe, links] */
const NOS = {
  estudante: [0, 60, 190, 'Cliente', 'Estudante', 'envia a avaliação',
    'Qualquer cliente HTTP que tenha a API key: Postman, curl ou esta apresentação.', []],
  api: [235, 60, 215, 'API Gateway', 'REST API', 'POST /avaliacao',
    'Porta de entrada em HTTPS. Exige x-api-key e aplica o plano de uso: 5 req/s e 10 mil chamadas por mês.',
    [['api', 'Abrir a API'], ['planos', 'Plano de uso']]],
  receber: [495, 60, 260, 'Lambda', 'receber-avaliacao', 'valida · classifica · grava',
    'Valida descrição e nota, calcula a urgência, grava no DynamoDB e publica as críticas na fila.',
    [['fnReceber', 'Função'], ['logsReceber', 'Logs']]],
  sqs: [800, 60, 225, 'SQS', 'avaliacoes-criticas', 'fila das críticas',
    'Separa o recebimento do envio do e-mail. Criptografada, com reentrega automática em caso de falha.',
    [['filas', 'Abrir filas']]],
  notificar: [1070, 60, 235, 'Lambda', 'notificar-urgencia', 'monta e envia o alerta',
    'Consome uma mensagem por vez e envia o alerta ao administrador. Só tem permissão para ses:SendEmail.',
    [['fnNotificar', 'Função'], ['logsNotificar', 'Logs']]],
  ses: [1350, 60, 190, 'SES', 'Amazon SES', 'envio de e-mail',
    'Serviço de e-mail da AWS. Remetente e destinatário vêm do Parameter Store.',
    [['ses', 'Identidades']]],
  admin: [1585, 60, 195, 'Destino', 'Administrador', 'recebe por e-mail',
    'Recebe o alerta das avaliações críticas e, toda segunda-feira, o relatório semanal.', []],
  dynamo: [495, 300, 260, 'DynamoDB', 'avaliacoes', 'um dia por partição',
    'Tabela feedback-serverless-avaliacoes. A chave de partição é o dia, o que permite ler uma semana com 7 Queries.',
    [['itens', 'Ver os itens']]],
  dlq1: [800, 235, 225, 'DLQ', 'criticas-dlq', '',
    'feedback-serverless-avaliacoes-criticas-dlq: recebe a mensagem depois de 3 tentativas sem sucesso e a guarda por 14 dias.',
    [['filas', 'Abrir filas'], ['alarmes', 'Alarmes']]],
  dlq2: [800, 420, 225, 'DLQ', 'relatorio-dlq', '',
    'feedback-serverless-relatorio-semanal-dlq: guarda as execuções do relatório que falharam em definitivo.',
    [['filas', 'Abrir filas'], ['alarmes', 'Alarmes']]],
  scheduler: [235, 540, 215, 'EventBridge', 'Scheduler', 'segunda · 08:00',
    'Agenda cron(0 8 ? * MON *) no fuso de São Paulo. Tenta de novo até 2 vezes se a invocação falhar.',
    [['agenda', 'Agendamento']]],
  relatorio: [495, 540, 260, 'Lambda', 'gerar-relatorio-semanal', 'consolida 7 dias',
    'Lê os sete dias anteriores, calcula média e totais por dia e por urgência, e envia o relatório.',
    [['fnRelatorio', 'Função'], ['logsRelatorio', 'Logs']]],
};

/** Componentes de apoio, agrupados em dois quadros: [x, y, largura, nome, texto, links] */
const APOIO = {
  logs: [1086, 290, 142, 'Logs', 'Um log group por função no CloudWatch, com retenção de 7 dias.', [['logsReceber', 'Abrir logs']]],
  alarmes: [1242, 290, 142, 'Alarmes', '5 alarmes: erro em cada Lambda e mensagem em cada DLQ.', [['alarmes', 'Alarmes']]],
  dashboard: [1086, 346, 142, 'Dashboard', 'Invocações, erros, DLQs e throttling do DynamoDB num painel só.', [['dashboard', 'Dashboard']]],
  xray: [1242, 346, 142, 'X-Ray', 'Rastreamento ativo na API e nas três funções.', [['xray', 'Mapa de rastreamento']]],
  ssm: [1506, 290, 258, 'Parameter Store', 'Guarda os e-mails de remetente e administrador, fora do código.', [['parametros', 'Parâmetros']]],
  actions: [1506, 346, 258, 'GitHub Actions', 'Compila, testa e faz o deploy a cada push na main.', [['actions', 'Execuções'], ['workflow', 'ci-cd.yml']]],
  sam: [1506, 402, 258, 'SAM · CloudFormation', 'Toda a infraestrutura descrita em template.yaml, numa única stack.', [['stack', 'Abrir a stack'], ['template', 'template.yaml']]],
};

const GRUPOS = [
  [1070, 250, 330, 154, 'Observabilidade'],
  [1490, 250, 290, 210, 'Configuração e deploy'],
];

/** id: [caminho SVG, tracejada?] */
const ARESTAS = {
  'estudante-api': ['M190,110 H233'],
  'api-receber': ['M450,110 H493'],
  'receber-sqs': ['M755,110 H798'],
  'sqs-notificar': ['M1025,110 H1068'],
  'notificar-ses': ['M1305,110 H1348'],
  'ses-admin': ['M1540,110 H1583'],
  'receber-dynamo': ['M625,160 V298'],
  'sqs-dlq1': ['M912,160 V233', true],
  'relatorio-dynamo': ['M625,540 V402'],
  'scheduler-relatorio': ['M450,590 H493'],
  'relatorio-ses': ['M755,610 H1421 Q1445,610 1445,586 V162'],
  'relatorio-dlq2': ['M755,572 C790,572 768,458 798,458', true],
};

/** [x, y, texto, destaque?] */
const ROTULOS = [
  [777, 50, 'se CRÍTICA', true, 'middle'],
  [639, 234, 'PutItem'],
  [926, 202, '3 falhas'],
  [639, 476, '7 Queries'],
  [1090, 598, 'SendEmail'],
];

const FLUXOS = {
  comum: {
    rotulo: 'Avaliação comum',
    inicio: 'estudante',
    abertura: 'O estudante envia POST /avaliacao com nota 9.',
    passos: [
      ['estudante-api', 'api', 'O API Gateway confere a x-api-key e o plano de uso.'],
      ['api-receber', 'receber', 'A Lambda valida os dados e classifica: nota 9 é BAIXA.'],
      ['receber-dynamo', 'dynamo', 'Grava no DynamoDB e responde 201. A avaliação entra no relatório semanal.'],
    ],
  },
  critica: {
    rotulo: 'Avaliação crítica',
    inicio: 'estudante',
    abertura: 'O estudante envia POST /avaliacao com nota 2.',
    passos: [
      ['estudante-api', 'api', 'O API Gateway confere a x-api-key e o plano de uso.'],
      ['api-receber', 'receber', 'A Lambda valida os dados e classifica: nota 2 é CRITICA.'],
      ['receber-dynamo', 'dynamo', 'Grava no DynamoDB, como qualquer avaliação.'],
      ['receber-sqs', 'sqs', 'Por ser crítica, também publica uma mensagem na fila SQS.'],
      ['sqs-notificar', 'notificar', 'A fila dispara notificar-urgencia, uma mensagem por vez.'],
      ['notificar-ses', 'ses', 'A função monta o alerta e entrega ao SES.'],
      ['ses-admin', 'admin', 'O administrador recebe o e-mail com descrição, urgência e data de envio.'],
    ],
  },
  falha: {
    rotulo: 'Falha no envio',
    inicio: 'sqs',
    abertura: 'Uma mensagem crítica está na fila, mas o envio do e-mail está falhando.',
    passos: [
      ['sqs-notificar', 'notificar', 'Primeira tentativa: a função falha e a mensagem volta para a fila.'],
      ['sqs-notificar', 'notificar', 'Segunda e terceira tentativas: falham de novo.'],
      ['sqs-dlq1', 'dlq1', 'A mensagem vai para a DLQ, fica guardada por 14 dias e o alarme dispara.'],
    ],
  },
  relatorio: {
    rotulo: 'Relatório semanal',
    inicio: 'scheduler',
    abertura: 'Segunda-feira, 08:00 no horário de Brasília.',
    passos: [
      ['scheduler-relatorio', 'relatorio', 'O Scheduler invoca gerar-relatorio-semanal.'],
      ['relatorio-dynamo', 'dynamo', 'A função faz 7 Queries, uma por dia, sem Scan na tabela.'],
      ['relatorio-ses', 'ses', 'Calcula média e totais por dia e por urgência, e envia pelo SES.'],
      ['ses-admin', 'admin', 'O administrador recebe o relatório da semana.'],
    ],
  },
};

const desenharNo = ([x, y, w, tipo, nome, sub], id) => {
  const h = sub ? ALTURA_NO : 76;
  return `
    <g class="no" data-no="${id}" tabindex="0" role="button" aria-label="${tipo}: ${nome}" transform="translate(${x},${y})">
      <rect width="${w}" height="${h}" rx="14"/>
      <text class="tipo" x="18" y="30">${tipo.toUpperCase()}</text>
      <text class="nome" x="18" y="${sub ? 58 : 54}">${nome}</text>
      ${sub ? `<text class="sub" x="18" y="81">${sub}</text>` : ''}
    </g>`;
};

const desenharApoio = ([x, y, w, nome], id) => `
    <g class="no chip" data-no="${id}" tabindex="0" role="button" aria-label="${nome}" transform="translate(${x},${y})">
      <rect width="${w}" height="42" rx="10"/>
      <text class="nome" x="${w / 2}" y="27" text-anchor="middle">${nome}</text>
    </g>`;

const seta = (id, cor) => `
    <marker id="${id}" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0,0 L10,5 L0,10 z" fill="${cor}"/>
    </marker>`;

export function montarDiagrama(raiz, slide) {
  const semMovimento = matchMedia('(prefers-reduced-motion: reduce)');
  const esperar = (ms) => new Promise((ok) => setTimeout(ok, semMovimento.matches ? 0 : ms));

  raiz.innerHTML = `
    <div class="diag-topo">
      <span class="rotulo">Animar</span>
      ${Object.entries(FLUXOS).map(([id, f]) => `<button type="button" class="botao" data-fluxo="${id}">${f.rotulo}</button>`).join('')}
      <span class="dica">clique num componente para ver o detalhe</span>
    </div>
    <svg viewBox="0 34 1780 618" role="group" aria-label="Diagrama da arquitetura">
      <defs>${seta('seta', '#4a5059')}${seta('seta-rosa', '#ed145b')}</defs>
      ${GRUPOS.map(([x, y, w, h, titulo]) => `
        <g class="grupo"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="14"/><text x="${x + 16}" y="${y + 25}">${titulo.toUpperCase()}</text></g>`).join('')}
      ${Object.entries(ARESTAS).map(([id, [d, tracejada]]) =>
        `<path class="aresta${tracejada ? ' tracejada' : ''}" data-aresta="${id}" d="${d}" marker-end="url(#seta)"/>`).join('')}
      ${ROTULOS.map(([x, y, texto, forte, ancora]) =>
        `<text class="rot${forte ? ' forte' : ''}" x="${x}" y="${y}"${ancora ? ` text-anchor="${ancora}"` : ''}>${texto}</text>`).join('')}
      ${Object.entries(NOS).map(([id, no]) => desenharNo(no, id)).join('')}
      ${Object.entries(APOIO).map(([id, no]) => desenharApoio(no, id)).join('')}
      <circle class="pacote" r="7" visibility="hidden"/>
    </svg>
    <p class="diag-legenda" aria-live="polite">Escolha um fluxo para ver o caminho, passo a passo.</p>`;

  const svg = raiz.querySelector('svg');
  const legenda = raiz.querySelector('.diag-legenda');
  const pacote = raiz.querySelector('.pacote');
  const no = (id) => svg.querySelector(`[data-no="${id}"]`);
  const aresta = (id) => svg.querySelector(`[data-aresta="${id}"]`);
  let execucao = 0;
  let detalhe = null;

  function marcarAresta(id, estado) {
    const el = aresta(id);
    el.classList.remove('ativa', 'feita');
    if (estado) el.classList.add(estado);
    el.setAttribute('marker-end', `url(#${estado ? 'seta-rosa' : 'seta'})`);
  }

  function limpar() {
    svg.querySelectorAll('.no').forEach((el) => el.classList.remove('ativo', 'feito'));
    Object.keys(ARESTAS).forEach((id) => marcarAresta(id, null));
    pacote.setAttribute('visibility', 'hidden');
  }

  function ativarNo(id) {
    svg.querySelectorAll('.no.ativo').forEach((el) => el.classList.replace('ativo', 'feito'));
    no(id).classList.remove('feito');
    no(id).classList.add('ativo');
  }

  /** Leva o "pacote" de uma ponta à outra da aresta. */
  function viajar(id, minha) {
    const caminho = aresta(id);
    marcarAresta(id, 'ativa');
    if (semMovimento.matches) return Promise.resolve();

    const comprimento = caminho.getTotalLength();
    const duracao = Math.min(1500, 420 + comprimento * 0.9);
    const inicio = performance.now();
    pacote.setAttribute('visibility', 'visible');

    return new Promise((ok) => {
      function quadro(agora) {
        if (minha !== execucao) return ok();
        const t = Math.min(1, (agora - inicio) / duracao);
        const suave = t < 0.5 ? 2 * t * t : 1 - (-2 * t + 2) ** 2 / 2;
        const ponto = caminho.getPointAtLength(suave * comprimento);
        pacote.setAttribute('cx', ponto.x);
        pacote.setAttribute('cy', ponto.y);
        if (t < 1) requestAnimationFrame(quadro);
        else ok();
      }
      requestAnimationFrame(quadro);
    });
  }

  async function tocar(id) {
    const minha = ++execucao;
    const fluxo = FLUXOS[id];
    fecharDetalhe();
    limpar();
    raiz.querySelectorAll('[data-fluxo]').forEach((b) => b.classList.toggle('ativo', b.dataset.fluxo === id));

    ativarNo(fluxo.inicio);
    legenda.textContent = fluxo.abertura;
    for (const [idAresta, destino, texto] of fluxo.passos) {
      await esperar(1100);
      if (minha !== execucao) return;
      await viajar(idAresta, minha);
      if (minha !== execucao) return;
      pacote.setAttribute('visibility', 'hidden');
      marcarAresta(idAresta, 'feita');
      ativarNo(destino);
      legenda.textContent = texto;
    }
  }

  function fecharDetalhe() {
    detalhe?.remove();
    detalhe = null;
  }

  function abrirDetalhe(id) {
    const aberto = detalhe?.dataset.no === id;
    fecharDetalhe();
    if (aberto) return;

    const principal = NOS[id];
    const [tipo, nome, texto, links] = principal
      ? [principal[3], principal[4], principal[6], principal[7]]
      : ['Apoio', APOIO[id][3], APOIO[id][4], APOIO[id][5]];

    detalhe = document.createElement('aside');
    detalhe.className = 'diag-detalhe';
    detalhe.dataset.no = id;
    detalhe.innerHTML = `
      <small>${tipo}</small>
      <h3>${nome}</h3>
      <p>${texto}</p>
      <div class="links">${links.map(([chave, rotulo]) => link(chave, rotulo)).join('')}</div>`;
    raiz.append(detalhe);
    preencherLinks(detalhe);
  }

  raiz.addEventListener('click', (e) => {
    const botao = e.target.closest('[data-fluxo]');
    if (botao) return tocar(botao.dataset.fluxo);
    const clicado = e.target.closest('[data-no]');
    if (clicado && svg.contains(clicado)) return abrirDetalhe(clicado.dataset.no);
    if (!e.target.closest('.diag-detalhe')) fecharDetalhe();
  });
  svg.addEventListener('keydown', (e) => {
    const focado = e.target.closest('[data-no]');
    if (focado && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      abrirDetalhe(focado.dataset.no);
    }
  });

  slide.addEventListener('sair', () => {
    execucao++;
    fecharDetalhe();
    limpar();
    raiz.querySelectorAll('[data-fluxo]').forEach((b) => b.classList.remove('ativo'));
    legenda.textContent = 'Escolha um fluxo para ver o caminho, passo a passo.';
  });
}
