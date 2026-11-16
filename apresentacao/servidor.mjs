#!/usr/bin/env node
// Servidor local da apresentação.
//
// Faz duas coisas:
//   1. serve os arquivos estáticos desta pasta;
//   2. repassa as requisições da demo para a API na AWS.
//
// O repasse existe porque a API Gateway não tem CORS: um navegador não consegue
// chamá-la direto de uma página. Só as requisições da collection Postman de
// docs/ podem ser enviadas, sempre para o host da própria collection.
//
// Uso:  node apresentacao/servidor.mjs        (PORT=xxxx para trocar a porta)

import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = fileURLToPath(new URL('.', import.meta.url));
const COLLECTION = resolve(RAIZ, '..', 'docs', 'Tech_Challenge_4_Postman_Collection.json');
const HOST = '127.0.0.1';
const PORTA = Number(process.env.PORT ?? 4173);
const HOST_API_PERMITIDO = /^[a-z0-9]+\.execute-api\.[a-z0-9-]+\.amazonaws\.com$/;
const TEMPO_LIMITE_MS = 15_000;
const TAMANHO_MAXIMO_CORPO = 64 * 1024;

const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
};

/** Lê a collection Postman e devolve só o que a apresentação precisa. */
async function lerCollection() {
  const bruta = JSON.parse(await readFile(COLLECTION, 'utf8'));
  const variaveis = Object.fromEntries((bruta.variable ?? []).map((v) => [v.key, v.value]));
  const baseUrl = String(variaveis.baseUrl ?? '').replace(/\/+$/, '');

  const itens = (bruta.item ?? []).map((item, indice) => {
    const requisicao = item.request ?? {};
    const testes = (item.event ?? [])
      .filter((e) => e.listen === 'test')
      .flatMap((e) => e.script?.exec ?? [])
      .join('\n');
    const cabecalhos = (requisicao.header ?? []).map((h) => ({ chave: h.key, valor: h.value }));

    return {
      indice,
      nome: item.name,
      metodo: requisicao.method ?? 'GET',
      url: String(requisicao.url?.raw ?? '').replace('{{baseUrl}}', baseUrl),
      cabecalhos,
      usaChave: cabecalhos.some((h) => h.chave.toLowerCase() === 'x-api-key'),
      corpo: requisicao.body?.raw ?? '',
      statusEsperado: Number(testes.match(/to\.have\.status\((\d+)\)/)?.[1]) || null,
      urgenciaEsperada: testes.match(/urgencia\)\.to\.eql\("(\w+)"\)/)?.[1] ?? null,
    };
  });

  return { nome: bruta.info?.name ?? 'Collection', baseUrl, itens };
}

function responderJson(res, status, dados) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(dados));
}

async function lerCorpo(req) {
  const partes = [];
  let tamanho = 0;
  for await (const parte of req) {
    tamanho += parte.length;
    if (tamanho > TAMANHO_MAXIMO_CORPO) throw new Error('corpo grande demais');
    partes.push(parte);
  }
  return Buffer.concat(partes).toString('utf8');
}

/** Envia para a AWS uma requisição da collection (o corpo pode ter sido editado na tela). */
async function enviar(req, res) {
  // Cabeçalho próprio: uma página de outro site não consegue mandá-lo sem preflight.
  if (req.headers['x-apresentacao'] !== '1') {
    return responderJson(res, 403, { erro: 'requisição não veio da apresentação' });
  }

  let pedido;
  try {
    pedido = JSON.parse(await lerCorpo(req));
  } catch {
    return responderJson(res, 400, { erro: 'pedido inválido' });
  }

  const { itens } = await lerCollection();
  const item = itens[pedido.indice];
  if (!item) return responderJson(res, 404, { erro: 'requisição não existe na collection' });

  let destino;
  try {
    destino = new URL(item.url);
  } catch {
    return responderJson(res, 400, { erro: 'URL da collection inválida' });
  }
  if (destino.protocol !== 'https:' || !HOST_API_PERMITIDO.test(destino.hostname)) {
    return responderJson(res, 400, { erro: `host não permitido: ${destino.hostname}` });
  }

  const chave = String(req.headers['x-api-key'] ?? '');
  const cabecalhos = {};
  for (const { chave: nome, valor } of item.cabecalhos) {
    if (nome.toLowerCase() === 'x-api-key') {
      if (chave) cabecalhos['x-api-key'] = chave;
    } else {
      cabecalhos[nome] = valor;
    }
  }

  const corpo = typeof pedido.corpo === 'string' ? pedido.corpo : item.corpo;
  const inicio = performance.now();
  try {
    const resposta = await fetch(destino, {
      method: item.metodo,
      headers: cabecalhos,
      body: ['GET', 'HEAD'].includes(item.metodo) ? undefined : corpo,
      signal: AbortSignal.timeout(TEMPO_LIMITE_MS),
    });
    const texto = await resposta.text();
    const ms = Math.round(performance.now() - inicio);
    const expostos = ['content-type', 'x-amzn-requestid', 'x-amzn-errortype', 'x-amzn-trace-id'];
    responderJson(res, 200, {
      status: resposta.status,
      statusTexto: resposta.statusText,
      ms,
      cabecalhos: Object.fromEntries(expostos.filter((c) => resposta.headers.has(c)).map((c) => [c, resposta.headers.get(c)])),
      corpo: texto,
    });
  } catch (erro) {
    responderJson(res, 502, { erro: `não consegui falar com a API: ${erro.cause?.code ?? erro.name}` });
  }
}

async function servirArquivo(caminho, res) {
  const relativo = caminho === '/' ? 'index.html' : decodeURIComponent(caminho.slice(1));
  const arquivo = resolve(RAIZ, relativo);
  const tipo = TIPOS[extname(arquivo)];
  if (!tipo || !(arquivo + sep).startsWith(RAIZ)) {
    res.writeHead(404).end('não encontrado');
    return;
  }
  try {
    const conteudo = await readFile(arquivo);
    res.writeHead(200, { 'Content-Type': tipo, 'Cache-Control': 'no-store' });
    res.end(conteudo);
  } catch {
    res.writeHead(404).end('não encontrado');
  }
}

const servidor = createServer(async (req, res) => {
  try {
    // Só atende quem chama pelo endereço local (bloqueia DNS rebinding).
    const hostPedido = String(req.headers.host ?? '').split(':')[0];
    if (!['localhost', '127.0.0.1'].includes(hostPedido)) {
      res.writeHead(403).end('acesso negado');
      return;
    }

    const { pathname } = new URL(req.url, `http://${req.headers.host}`);
    if (req.method === 'GET' && pathname === '/api/collection') {
      return responderJson(res, 200, await lerCollection());
    }
    if (req.method === 'POST' && pathname === '/api/enviar') {
      return await enviar(req, res);
    }
    if (req.method === 'GET') return await servirArquivo(pathname, res);
    res.writeHead(405).end('método não permitido');
  } catch (erro) {
    console.error(erro);
    if (!res.headersSent) responderJson(res, 500, { erro: 'erro interno do servidor local' });
  }
});

servidor.on('error', (erro) => {
  if (erro.code === 'EADDRINUSE') {
    console.error(`A porta ${PORTA} já está em uso. Tente: PORT=4174 node apresentacao/servidor.mjs`);
  } else {
    console.error(erro);
  }
  process.exit(1);
});

servidor.listen(PORTA, HOST, () => {
  console.log(`\n  Apresentação no ar:  http://localhost:${PORTA}\n  (Ctrl+C para encerrar)\n`);
});
