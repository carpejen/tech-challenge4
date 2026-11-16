// Fundo animado: um "terreno" de pontos em perspectiva que ondula devagar,
// com um brilho rosa que muda de lugar a cada slide.

const ROSA = '237,20,91';
const CINZA = '172,193,204';

export function iniciarFundo(canvas) {
  const ctx = canvas.getContext('2d');
  const semMovimento = matchMedia('(prefers-reduced-motion: reduce)');
  let largura = 0;
  let altura = 0;
  let escala = 1;
  let tempo = 0;
  let quadro = 0;
  const foco = { x: 0.75, y: 0.3 };
  const alvo = { x: 0.75, y: 0.3 };

  function redimensionar() {
    escala = Math.min(devicePixelRatio || 1, 2);
    largura = canvas.width = Math.round(innerWidth * escala);
    altura = canvas.height = Math.round(innerHeight * escala);
    desenhar();
  }

  function desenhar() {
    ctx.clearRect(0, 0, largura, altura);

    const gx = foco.x * largura;
    const gy = foco.y * altura;
    const brilho = ctx.createRadialGradient(gx, gy, 0, gx, gy, Math.max(largura, altura) * 0.55);
    brilho.addColorStop(0, `rgba(${ROSA},.20)`);
    brilho.addColorStop(0.45, `rgba(${ROSA},.05)`);
    brilho.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = brilho;
    ctx.fillRect(0, 0, largura, altura);

    const colunas = 54;
    const linhas = 26;
    const horizonte = altura * 0.46;
    for (let j = 0; j < linhas; j++) {
      const z = j / (linhas - 1); // 0 = longe, 1 = perto
      const perspectiva = 0.16 + z * z * 1.05;
      const yBase = horizonte + altura * 0.6 * z * z;
      for (let i = 0; i < colunas; i++) {
        const x = (i / (colunas - 1) - 0.5) * largura * 2.1 * perspectiva + largura / 2;
        if (x < -20 || x > largura + 20) continue;
        const onda = Math.sin(i * 0.4 + tempo * 0.9 + j * 0.33) * Math.cos(j * 0.28 - tempo * 0.55);
        const y = yBase - onda * 30 * escala * perspectiva;
        const lado = (0.7 + perspectiva * 2) * escala;
        const destaque = (i * 7 + j * 13) % 31 === 0;
        const alfa = Math.max(0, 0.08 + z * 0.34 + onda * 0.1);
        ctx.fillStyle = destaque ? `rgba(${ROSA},${Math.min(1, alfa + 0.45)})` : `rgba(${CINZA},${alfa})`;
        ctx.fillRect(x, y, destaque ? lado * 1.6 : lado, destaque ? lado * 1.6 : lado);
      }
    }
  }

  function animar() {
    tempo += 0.012;
    foco.x += (alvo.x - foco.x) * 0.03;
    foco.y += (alvo.y - foco.y) * 0.03;
    desenhar();
    quadro = requestAnimationFrame(animar);
  }

  function ligar() {
    cancelAnimationFrame(quadro);
    if (semMovimento.matches || document.hidden) {
      foco.x = alvo.x;
      foco.y = alvo.y;
      desenhar();
    } else {
      quadro = requestAnimationFrame(animar);
    }
  }

  addEventListener('resize', redimensionar);
  document.addEventListener('visibilitychange', ligar);
  semMovimento.addEventListener('change', ligar);
  redimensionar();
  ligar();

  return {
    /** Move o brilho para um ponto da tela (0 a 1 em cada eixo). */
    focar(x, y) {
      alvo.x = x;
      alvo.y = y;
      if (semMovimento.matches) ligar();
    },
  };
}
