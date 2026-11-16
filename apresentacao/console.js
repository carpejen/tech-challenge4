// Console de API (slide 10): envia as requisições da collection Postman de docs/.
//
// As chamadas reais passam pelo servidor local (servidor.mjs), porque a API
// Gateway não tem CORS. A API key fica só na memória desta aba (sessionStorage)
// e é enviada ao servidor local num cabeçalho; nunca é gravada em arquivo.

import { PROJETO, link, preencherLinks } from './conteudo.js';

const CHAVE_SESSAO = 'tc4.apiKey';
const ETAPAS = ['API Gateway', 'Lambda', 'DynamoDB', 'SQS', 'Lambda', 'SES'];
const TEXTO_STATUS = { 200: 'OK', 201: 'Created', 400: 'Bad Request', 403: 'Forbidden', 429: 'Too Many Requests', 500: 'Internal Server Error', 502: 'Bad Gateway' };

const escapar = (texto) => String(texto).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const lerJson = (texto) => { try { return JSON.parse(texto); } catch { return undefined; } };

/** JSON indentado e colorido. */
function formatar(valor, nivel = 0) {
  const recuo = '  '.repeat(nivel);
  if (valor === null || typeof valor === 'number' || typeof valor === 'boolean') return `<span class="n">${valor}</span>`;
  if (typeof valor === 'string') return `<span class="s">${escapar(JSON.stringify(valor))}</span>`;
  const lista = Array.isArray(valor);
  const linhas = lista
    ? valor.map((v) => `${recuo}  ${formatar(v, nivel + 1)}`)
    : Object.entries(valor).map(([k, v]) => `${recuo}  <span class="k">${escapar(JSON.stringify(k))}</span>: ${formatar(v, nivel + 1)}`);
  const [abre, fecha] = lista ? ['[', ']'] : ['{', '}'];
  return linhas.length ? `${abre}\n${linhas.join(',\n')}\n${recuo}${fecha}` : abre + fecha;
}

/**
 * Modo ensaio: responde como a API responderia, sem chamar a AWS.
 * Repete as validações de ReceberAvaliacaoHandler, Avaliacao e Urgencia.
 */
function simular(item, corpo) {
  const erro = (mensagem) => ({ status: 400, corpo: JSON.stringify({ erro: mensagem }) });
  if (!item.usaChave) return { status: 403, corpo: JSON.stringify({ message: 'Forbidden' }) };
  if (!corpo.trim()) return erro('corpo da requisição é obrigatório');

  const dados = lerJson(corpo);
  if (dados === undefined) return erro('JSON inválido');
  if (dados === null || typeof dados !== 'object' || Array.isArray(dados)) return erro('o corpo deve ser um objeto JSON');

  const { descricao, nota } = dados;
  if (descricao != null && typeof descricao !== 'string') return erro('descricao deve ser texto');
  if (nota != null && !Number.isInteger(nota)) return erro('nota deve ser um número inteiro');
  if (descricao == null || !descricao.trim()) return erro('descricao é obrigatória');
  if (descricao.trim().length > 1000) return erro('descricao deve ter no máximo 1000 caracteres');
  if (nota == null) return erro('nota é obrigatória');
  if (nota < 0 || nota > 10) return erro('nota deve estar entre 0 e 10');

  return {
    status: 201,
    corpo: JSON.stringify({
      id: crypto.randomUUID(),
      descricao: descricao.trim(),
      nota,
      urgencia: nota <= 4 ? 'CRITICA' : nota <= 7 ? 'MEDIA' : 'BAIXA',
      dataEnvio: new Date().toISOString(),
    }),
  };
}

export function montarConsole(raiz) {
  const semMovimento = matchMedia('(prefers-reduced-motion: reduce)');
  const esperar = (ms) => new Promise((ok) => setTimeout(ok, semMovimento.matches ? 0 : ms));

  raiz.innerHTML = `
    <div class="con-barra">
      <label for="con-chave">API key</label>
      <input id="con-chave" class="con-chave" type="password" autocomplete="off" spellcheck="false" placeholder="cole a chave antes de gravar" data-1p-ignore data-lpignore="true">
      <span class="con-estado" data-estado></span>
      <label class="con-ensaio"><span>Modo ensaio</span><input type="checkbox" data-ensaio></label>
    </div>
    <div class="con-grade">
      <section class="con-p">
        <div class="con-h"><span>Collection Postman</span><span data-contagem></span></div>
        <div class="con-lista" data-lista></div>
        <div class="con-acoes"><button type="button" class="botao" data-todas>Rodar todas</button></div>
      </section>
      <section class="con-p">
        <div class="con-h"><span>Requisição</span><span>corpo editável</span></div>
        <div class="con-url" data-url></div>
        <div class="con-cab" data-cab></div>
        <textarea class="con-corpo" data-corpo spellcheck="false" aria-label="Corpo da requisição"></textarea>
        <div class="con-acoes"><button type="button" class="botao cheio" data-enviar>Enviar</button></div>
      </section>
      <section class="con-p">
        <div class="con-h"><span>Resposta</span><span data-origem></span></div>
        <div class="con-status" data-status></div>
        <pre class="con-json" data-json></pre>
        <div class="con-caminho" data-caminho>${ETAPAS.map((e) => `<span>${e}</span>`).join('')}</div>
        <div class="con-rodape" data-rodape></div>
      </section>
    </div>`;

  const $ = (seletor) => raiz.querySelector(seletor);
  const campoChave = $('.con-chave');
  const ensaio = $('[data-ensaio]');
  const lista = $('[data-lista]');
  const campoCorpo = $('[data-corpo]');
  const botaoEnviar = $('[data-enviar]');
  const botaoTodas = $('[data-todas]');

  let itens = [];
  let selecionado = 0;
  let ocupado = false;
  const corpos = new Map(); // corpo editado de cada requisição
  const resultados = new Map();

  campoChave.value = sessionStorage.getItem(CHAVE_SESSAO) ?? '';

  // ---------------------------------------------------------- desenho

  function desenharEstado() {
    const estado = $('[data-estado]');
    estado.classList.toggle('ok', !ensaio.checked && Boolean(campoChave.value));
    estado.textContent = ensaio.checked
      ? 'ensaio: nada é enviado à AWS'
      : campoChave.value ? 'chave pronta' : 'sem chave: a API responde 403';
  }

  function desenharLista() {
    $('[data-contagem]').textContent = `${itens.length} requisições`;
    lista.innerHTML = itens.map((item, i) => {
      const r = resultados.get(i);
      const marca = r?.enviando ? 'indo' : r ? (r.passou === false ? 'falhou' : 'passou') : '';
      return `
        <button type="button" class="con-item${i === selecionado ? ' sel' : ''}" data-item="${i}">
          <span>${escapar(item.nome)}</span>
          <em>${item.statusEsperado ?? ''}</em>
          <i class="${marca}"></i>
        </button>`;
    }).join('');
  }

  function desenharRequisicao() {
    const item = itens[selecionado];
    const endereco = new URL(item.url);
    $('[data-url]').innerHTML = `<b>${escapar(item.metodo)}</b>${escapar(endereco.pathname)}<small>${escapar(endereco.host)}</small>`;

    const linhas = item.cabecalhos
      .filter((c) => c.chave.toLowerCase() !== 'x-api-key')
      .map((c) => `<div><span>${escapar(c.chave)}:</span> ${escapar(c.valor)}</div>`);
    if (!item.usaChave) linhas.push('<div class="sem">x-api-key não é enviada nesta requisição</div>');
    else if (ensaio.checked) linhas.push('<div><span>x-api-key:</span> (ensaio)</div>');
    else if (campoChave.value) linhas.push('<div><span>x-api-key:</span> ••••••••••••</div>');
    else linhas.push('<div class="sem">x-api-key: (vazia)</div>');
    $('[data-cab]').innerHTML = linhas.join('');

    campoCorpo.value = corpos.get(selecionado) ?? item.corpo;
  }

  function desenharCaminho(resultado) {
    const etapas = [...$('[data-caminho]').children];
    etapas.forEach((e) => e.classList.remove('passou', 'parou'));
    if (!resultado || resultado.enviando || resultado.erroLocal) return;

    const { status } = resultado;
    const critica = lerJson(resultado.corpo)?.urgencia === 'CRITICA';
    // Até onde a requisição chegou, deduzido do status e da urgência da resposta.
    const ate = status === 403 || status === 429 ? 0 : status !== 201 ? 1 : critica ? 5 : 2;
    etapas.forEach((etapa, i) => {
      if (i > ate) return;
      setTimeout(() => etapa.classList.add(i === ate && status !== 201 ? 'parou' : 'passou'), semMovimento.matches ? 0 : i * 170);
    });
  }

  function desenharResposta() {
    const item = itens[selecionado];
    const r = resultados.get(selecionado);
    const status = $('[data-status]');
    const json = $('[data-json]');
    const rodape = $('[data-rodape]');
    status.className = 'con-status';
    rodape.innerHTML = '';
    $('[data-origem]').textContent = '';
    desenharCaminho(r);

    if (!r || r.enviando) {
      status.innerHTML = `<strong>···</strong><div>${r ? 'enviando…' : 'aguardando envio'}</div>`;
      json.innerHTML = `<span class="vazio">${r ? '' : `Esperado: ${item.statusEsperado ?? '—'}${item.urgenciaEsperada ? ` · urgência ${item.urgenciaEsperada}` : ''}`}</span>`;
      return;
    }
    if (r.erroLocal) {
      status.classList.add('s5');
      status.innerHTML = '<strong>!</strong><div>a chamada não saiu</div>';
      json.innerHTML = `<span class="vazio">${escapar(r.erroLocal)}</span>`;
      return;
    }

    const dados = lerJson(r.corpo);
    status.classList.add(r.status === 403 ? 's403' : `s${String(r.status)[0]}`);
    $('[data-origem]').textContent = r.simulado ? '' : `${r.ms} ms`;

    let veredito = '';
    if (r.simulado) veredito += '<span class="selo simulado">Simulado</span>';
    if (r.passou === true) veredito += '<span class="selo certo">como esperado</span>';
    if (r.passou === false) veredito += `<span class="selo errado">esperado ${item.statusEsperado}${item.urgenciaEsperada ? ` ${item.urgenciaEsperada}` : ''}</span>`;
    if (r.passou === null) veredito += '<span>corpo editado</span>';

    status.innerHTML = `
      <strong>${r.status}</strong>
      <div><span>${escapar(r.statusTexto || TEXTO_STATUS[r.status] || '')}</span>${dados?.urgencia ? `<span>urgência ${escapar(dados.urgencia)}</span>` : ''}</div>
      <div class="veredito">${veredito}</div>`;
    json.innerHTML = dados !== undefined ? formatar(dados) : escapar(r.corpo || '(sem corpo)');

    // Atalhos para conferir o efeito no console da AWS (só fazem sentido em chamada real).
    if (r.simulado) return;
    const atalhos = [];
    if (r.status === 201) atalhos.push(['itens', 'Ver no DynamoDB'], ['logsReceber', 'Logs']);
    if (dados?.urgencia === 'CRITICA') atalhos.push(['filas', 'Fila SQS'], ['logsNotificar', 'Logs do alerta']);
    if (r.status === 403) atalhos.push(['chaves', 'Chaves de API'], ['planos', 'Plano de uso']);
    if (r.status === 400) atalhos.push(['logsReceber', 'Logs da Lambda']);
    rodape.innerHTML = atalhos.map(([chave, rotulo]) => link(chave, rotulo)).join('');
    preencherLinks(rodape);
  }

  function selecionar(i) {
    selecionado = i;
    desenharLista();
    desenharRequisicao();
    desenharResposta();
  }

  function travar(valor) {
    ocupado = valor;
    botaoEnviar.disabled = valor;
    botaoTodas.disabled = valor;
  }

  // ---------------------------------------------------------- envio

  async function chamar(item, corpo) {
    if (ensaio.checked) {
      await esperar(380);
      return { ...simular(item, corpo), simulado: true };
    }
    const resposta = await fetch('/api/enviar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-apresentacao': '1', 'x-api-key': campoChave.value.trim() },
      body: JSON.stringify({ indice: item.indice, corpo }),
    });
    const dados = await resposta.json();
    return resposta.ok ? dados : { erroLocal: dados.erro ?? 'erro no servidor local' };
  }

  async function enviar(i) {
    const item = itens[i];
    const corpo = i === selecionado ? campoCorpo.value : corpos.get(i) ?? item.corpo;
    resultados.set(i, { enviando: true });
    if (i === selecionado) desenharResposta();
    desenharLista();

    let resultado;
    try {
      resultado = await chamar(item, corpo);
    } catch {
      resultado = { erroLocal: 'Servidor local fora do ar. Rode: node apresentacao/servidor.mjs' };
    }

    if (resultado.erroLocal) {
      resultado.passou = false;
    } else if (corpo !== item.corpo) {
      resultado.passou = null; // o corpo foi editado: a expectativa da collection não vale
    } else {
      const urgencia = lerJson(resultado.corpo)?.urgencia;
      resultado.passou = resultado.status === item.statusEsperado
        && (!item.urgenciaEsperada || urgencia === item.urgenciaEsperada);
    }
    resultados.set(i, resultado);
    desenharLista();
    if (i === selecionado) desenharResposta();
  }

  async function rodarTodas() {
    travar(true);
    for (let i = 0; i < itens.length; i++) {
      selecionar(i);
      await enviar(i);
      await esperar(900);
    }
    travar(false);
  }

  // ---------------------------------------------------------- eventos

  lista.addEventListener('click', (e) => {
    const botao = e.target.closest('[data-item]');
    if (botao && !ocupado) selecionar(Number(botao.dataset.item));
  });
  campoCorpo.addEventListener('input', () => corpos.set(selecionado, campoCorpo.value));
  campoCorpo.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) botaoEnviar.click();
  });
  botaoEnviar.addEventListener('click', async () => {
    if (ocupado) return;
    travar(true);
    await enviar(selecionado);
    travar(false);
  });
  botaoTodas.addEventListener('click', () => { if (!ocupado) rodarTodas(); });
  campoChave.addEventListener('input', () => {
    sessionStorage.setItem(CHAVE_SESSAO, campoChave.value.trim());
    desenharEstado();
    if (itens.length) desenharRequisicao();
  });
  ensaio.addEventListener('change', () => {
    resultados.clear();
    desenharEstado();
    if (itens.length) selecionar(selecionado);
  });

  // ---------------------------------------------------------- carga

  desenharEstado();
  fetch('/api/collection')
    .then((r) => (r.ok ? r.json() : Promise.reject()))
    .then((collection) => {
      itens = collection.itens;
      // O id da API vem do host da collection e alimenta os links do API Gateway.
      const apiId = collection.baseUrl.match(/^https:\/\/([a-z0-9]+)\.execute-api\./)?.[1];
      if (apiId) PROJETO.apiId = apiId;
      preencherLinks();
      selecionar(0);
    })
    .catch(() => {
      lista.innerHTML = '<p class="rodape" style="padding:20px">Não consegui ler a collection. Abra a apresentação pelo servidor local:<br><br><code>node apresentacao/servidor.mjs</code></p>';
      travar(true);
    });
}
