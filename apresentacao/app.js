// Motor da apresentação: monta os slides, cuida da navegação e liga as
// partes interativas (regra de urgência, pipeline, diagrama e console de API).

import { SLIDES, PIPELINE, preencherLinks } from './conteudo.js';
import { iniciarFundo } from './fundo.js';
import { montarDiagrama } from './diagrama.js';
import { montarConsole } from './console.js';

const LARGURA = 1920;
const ALTURA = 1080;

const palco = document.getElementById('palco');
const areaSlides = document.getElementById('slides');
const progresso = document.getElementById('progresso');
const indice = document.getElementById('indice');
const fundo = iniciarFundo(document.getElementById('fundo'));
const semMovimento = matchMedia('(prefers-reduced-motion: reduce)');

const dois = (n) => String(n).padStart(2, '0');
const esperar = (ms) => new Promise((ok) => setTimeout(ok, semMovimento.matches ? 0 : ms));

let atual = -1;

// ------------------------------------------------------------ montagem

function renderizar() {
  areaSlides.innerHTML = SLIDES.map((s, i) => `
    <section class="slide" id="slide-${s.id}" aria-label="${dois(i + 1)} — ${s.nome}">
      ${s.semCabecalho ? '' : `
        <header class="cab">
          <div class="kicker a"><b>${dois(i + 1)}</b>${s.secao}</div>
          <h2 class="a">${s.titulo}</h2>
        </header>`}
      ${s.corpo}
    </section>`).join('');

  // Atraso da entrada em cascata: cada .a recebe sua posição dentro do slide.
  for (const slide of areaSlides.children) {
    slide.querySelectorAll('.a').forEach((el, i) => el.style.setProperty('--i', i));
  }

  progresso.innerHTML = SLIDES.map((s, i) =>
    `<button type="button" title="${dois(i + 1)} · ${s.nome}" aria-label="Ir para o slide ${i + 1}: ${s.nome}"></button>`).join('');
  indice.innerHTML = SLIDES.map((s, i) =>
    `<button type="button"><b>${dois(i + 1)}</b><small>${s.secao}</small><strong>${s.nome}</strong></button>`).join('');
  document.getElementById('total').textContent = dois(SLIDES.length);
}

function ajustarEscala() {
  const escala = Math.min(innerWidth / LARGURA, innerHeight / ALTURA);
  palco.style.transform = `translate(-50%, -50%) scale(${escala})`;
}

// ------------------------------------------------------------ navegação

function ir(destino) {
  const n = Math.max(0, Math.min(SLIDES.length - 1, destino));
  if (n === atual) return;

  areaSlides.children[atual]?.dispatchEvent(new CustomEvent('sair'));
  atual = n;

  [...areaSlides.children].forEach((slide, i) => {
    slide.classList.toggle('ativa', i === atual);
    slide.classList.toggle('antes', i < atual);
    slide.inert = i !== atual;
  });
  [...progresso.children].forEach((b, i) => {
    b.classList.toggle('feito', i < atual);
    b.classList.toggle('atual', i === atual);
  });
  [...indice.children].forEach((b, i) => b.classList.toggle('atual', i === atual));

  document.getElementById('atual').textContent = dois(atual + 1);
  document.title = `${dois(atual + 1)} · ${SLIDES[atual].nome} — Feedback Serverless`;
  fundo.focar(...SLIDES[atual].foco);
  history.replaceState(null, '', `#/${atual + 1}`);
  areaSlides.children[atual].dispatchEvent(new CustomEvent('entrar'));
}

const slideDoEndereco = () => (Number(location.hash.match(/^#\/(\d+)/)?.[1]) || 1) - 1;

function alternarIndice(abrir = indice.hidden) {
  indice.hidden = !abrir;
  if (abrir) indice.children[atual]?.focus();
}

function alternarTelaCheia() {
  if (document.fullscreenElement) document.exitFullscreen();
  else document.documentElement.requestFullscreen?.();
}

function aoTeclar(evento) {
  if (evento.metaKey || evento.ctrlKey || evento.altKey) return;
  const alvo = evento.target instanceof Element ? evento.target : null;

  // Enquanto a pessoa digita num campo, as teclas são dela.
  if (alvo?.matches('input, textarea, select')) {
    if (evento.key === 'Escape') alvo.blur();
    return;
  }
  // Espaço e Enter num botão ou link acionam o próprio controle.
  if ((evento.key === ' ' || evento.key === 'Enter') && alvo?.closest('button, a, [role="button"]')) return;

  const acoes = {
    ArrowRight: () => ir(atual + 1),
    PageDown: () => ir(atual + 1),
    ' ': () => ir(atual + 1),
    ArrowLeft: () => ir(atual - 1),
    PageUp: () => ir(atual - 1),
    Home: () => ir(0),
    End: () => ir(SLIDES.length - 1),
    o: () => alternarIndice(),
    O: () => alternarIndice(),
    f: alternarTelaCheia,
    F: alternarTelaCheia,
    Escape: () => alternarIndice(false),
  };
  const acao = acoes[evento.key];
  if (!acao) return;
  evento.preventDefault();
  if (!indice.hidden && evento.key !== 'Escape' && evento.key.toLowerCase() !== 'o') alternarIndice(false);
  acao();
}

// ------------------------------------------------------------ interações dos slides

/** Slide "Da nota à urgência": a mesma regra do enum Urgencia do projeto. */
function montarUrgencia(raiz) {
  const notas = raiz.querySelector('[data-notas]');
  const resultado = raiz.querySelector('[data-resultado]');
  const cartao = raiz.querySelector('.urg-dir');
  const classificar = (nota) => (nota <= 4 ? 'CRITICA' : nota <= 7 ? 'MEDIA' : 'BAIXA');

  notas.innerHTML = Array.from({ length: 11 }, (_, nota) =>
    `<button type="button" role="radio" aria-checked="false" data-nota="${nota}" data-urg="${classificar(nota)}">${nota}</button>`).join('');

  function escolher(nota) {
    const urgencia = classificar(nota);
    for (const botao of notas.children) botao.setAttribute('aria-checked', String(Number(botao.dataset.nota) === nota));
    resultado.textContent = urgencia;
    cartao.dataset.urg = urgencia;
    const efeitos = { grava: true, relatorio: true, fila: urgencia === 'CRITICA', email: urgencia === 'CRITICA' };
    for (const item of raiz.querySelectorAll('[data-efeito]')) item.classList.toggle('nao', !efeitos[item.dataset.efeito]);
    const caminho = raiz.querySelector('[data-caminho-urg]');
    caminho.dataset.urg = urgencia;
    for (const etapa of caminho.children) {
      etapa.classList.toggle('on', urgencia === 'CRITICA' || !etapa.hasAttribute('data-so-critica'));
    }
    for (const faixa of raiz.querySelectorAll('.urg-faixas span')) {
      faixa.style.opacity = faixa.classList.contains(urgencia.toLowerCase()) ? 1 : 0.35;
    }
  }

  notas.addEventListener('click', (e) => {
    const botao = e.target.closest('[data-nota]');
    if (botao) escolher(Number(botao.dataset.nota));
  });
  escolher(2);
}

/** Slide de deploy: acende os passos do pipeline um a um. */
function montarPipeline(raiz, slide) {
  const abas = [...raiz.querySelectorAll('[data-pista]')];
  const passos = raiz.querySelector('[data-passos]');
  const fim = raiz.querySelector('[data-fim]');
  let execucao = 0;

  async function rodar(pista) {
    const minha = ++execucao;
    const { passos: lista, fim: texto } = PIPELINE[pista];
    abas.forEach((aba) => {
      const ativa = aba.dataset.pista === pista;
      aba.classList.toggle('ativo', ativa);
      aba.setAttribute('aria-selected', String(ativa));
    });
    passos.innerHTML = lista.map(([nome, descricao], i) =>
      `<li><small>Passo ${dois(i + 1)}</small><b>${nome}</b><span>${descricao}</span></li>`).join('');
    fim.textContent = texto;
    fim.classList.remove('visivel');

    for (const passo of passos.children) {
      await esperar(620);
      if (minha !== execucao) return;
      passo.previousElementSibling?.classList.replace('rodando', 'feito');
      passo.classList.add('rodando');
    }
    await esperar(760);
    if (minha !== execucao) return;
    passos.lastElementChild.classList.replace('rodando', 'feito');
    fim.classList.add('visivel');
  }

  abas.forEach((aba) => aba.addEventListener('click', () => rodar(aba.dataset.pista)));
  slide.addEventListener('entrar', () => rodar('main'));
  slide.addEventListener('sair', () => execucao++);
}

function montarCopiar(botao) {
  botao.addEventListener('click', async () => {
    const texto = botao.parentElement.querySelector('[data-comando]').textContent;
    try {
      await navigator.clipboard.writeText(texto);
      botao.textContent = 'Copiado';
    } catch {
      botao.textContent = 'Selecione e copie';
    }
    setTimeout(() => { botao.textContent = 'Copiar'; }, 1600);
  });
}

// ------------------------------------------------------------ início

renderizar();
ajustarEscala();
preencherLinks();

for (const slide of areaSlides.children) {
  slide.querySelectorAll('[data-urgencia]').forEach(montarUrgencia);
  slide.querySelectorAll('[data-pipeline]').forEach((el) => montarPipeline(el, slide));
  slide.querySelectorAll('[data-diagrama]').forEach((el) => montarDiagrama(el, slide));
  slide.querySelectorAll('[data-console]').forEach((el) => montarConsole(el));
  slide.querySelectorAll('[data-semana] li').forEach((li, n) => li.style.setProperty('--n', n));
  slide.querySelectorAll('[data-copiar]').forEach(montarCopiar);
}

addEventListener('resize', ajustarEscala);
addEventListener('keydown', aoTeclar);
addEventListener('hashchange', () => {
  alternarIndice(false);
  ir(slideDoEndereco());
});
progresso.addEventListener('click', (e) => {
  const i = [...progresso.children].indexOf(e.target.closest('button'));
  if (i >= 0) ir(i);
});
indice.addEventListener('click', (e) => {
  const i = [...indice.children].indexOf(e.target.closest('button'));
  if (i >= 0) ir(i);
  alternarIndice(false);
});

ir(slideDoEndereco());
