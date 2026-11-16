# Apresentação — Tech Challenge Fase 4

Apresentação animada e interativa do projeto, usada na gravação do vídeo de demonstração.
São 15 slides que cobrem os requisitos e os critérios de avaliação do enunciado, com um
console que chama a API de verdade e links diretos para os recursos no console da AWS.

## Como abrir

Na raiz do repositório (precisa só do Node 18 ou mais novo, sem instalar nada):

```bash
node apresentacao/servidor.mjs
```

Depois abra <http://localhost:4173>. Para usar outra porta: `PORT=4174 node apresentacao/servidor.mjs`.

Abrir o `index.html` direto no navegador não funciona: a página precisa do servidor local.

## Por que existe um servidor local

A API Gateway do projeto não tem CORS, então um navegador não consegue chamá-la a partir de
uma página. O `servidor.mjs` serve os arquivos e repassa as chamadas da demo para a AWS.
Ele só escuta em `127.0.0.1` e só envia as requisições que estão na collection Postman de
`docs/`, sempre para o host da própria collection.

## Atalhos

| Tecla | Ação |
|---|---|
| `→` `espaço` `PageDown` | Próximo slide |
| `←` `PageUp` | Slide anterior |
| `O` | Índice com todos os slides |
| `F` | Tela cheia |
| `Home` / `End` | Primeiro / último slide |

O endereço guarda o slide atual: `http://localhost:4173/#/10` abre direto na demo.

## Demo ao vivo (slide 10)

As requisições vêm de `docs/Tech_Challenge_4_Postman_Collection.json`, lidas a cada carregamento:
se a collection mudar, a apresentação acompanha.

1. Recupere o valor da API key (veja a seção 18 do README principal).
2. Cole no campo **API key** do slide. O campo é mascarado, então a chave não aparece no vídeo.
3. Clique em **Enviar** numa requisição, ou em **Rodar todas**.

A chave fica só na memória da aba (`sessionStorage`) e some quando a aba é fechada.
Nunca é gravada em arquivo.

O corpo da requisição é editável. Quando você altera o corpo, a comparação com o resultado
esperado da collection deixa de ser feita.

### Modo ensaio

Com **Modo ensaio** ligado, nada é enviado à AWS: as respostas são simuladas na própria página,
com as mesmas regras de validação do código, e aparecem com o selo **SIMULADO**. Serve para
treinar a apresentação sem gastar a cota da API. Deixe desligado na gravação final.

## Links da AWS

Os links abrem o console na região `us-east-2`. É preciso estar logado na conta do projeto
no mesmo navegador.

## Onde mexer

| Arquivo | O que tem |
|---|---|
| `conteudo.js` | Textos dos slides, links e a logo |
| `estilo.css` | Cores, tipografia e animações |
| `app.js` | Navegação, regra de urgência e pipeline |
| `diagrama.js` | Diagrama da arquitetura e os fluxos animados |
| `console.js` | Console de API e modo ensaio |
| `fundo.js` | Fundo animado |
| `servidor.mjs` | Servidor local e repasse das chamadas |
