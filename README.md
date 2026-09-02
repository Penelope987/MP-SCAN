# MP SCAN — GitHub Pages

Versão estática convertida do tema Blogger para hospedagem no GitHub Pages.

## Publicar
1. Crie um repositório, por exemplo `mp-scan`.
2. Envie `index.html`, `sw.js` e `manifest.webmanifest` para a raiz.
3. Em **Settings → Pages**, escolha **Deploy from a branch**, branch `main`, pasta `/ (root)`.
4. Abra o endereço HTTPS do GitHub Pages. O Service Worker só funciona em contexto seguro (HTTPS).

## Firebase
A configuração Firebase existente permanece no código JavaScript convertido. As Realtime Database Rules continuam sendo aplicadas no Firebase; o GitHub Pages é apenas a hospedagem do front-end.

## Offline
O app shell é cacheado pelo Service Worker e os capítulos que o sistema de download guardar localmente podem ser usados pelo leitor sem depender do Blogger. Recursos externos que não forem cacheáveis pelo navegador continuam dependendo da disponibilidade da rede.
