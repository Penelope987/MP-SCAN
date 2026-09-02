const CACHE = 'mp-scan-shell-v2';
const CORE = [
  './',
  './index.html',
  './manifest.webmanifest'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE)
      .then(cache => cache.addAll(CORE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(k => k !== CACHE && k.startsWith('mp-scan-shell-'))
        .map(k => caches.delete(k))
    )).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;

  event.respondWith((async () => {
    const cached = await caches.match(event.request, { ignoreSearch: false });
    if (cached) return cached;

    try {
      const response = await fetch(event.request);
      if (response && response.ok) {
        const copy = response.clone();
        const cache = await caches.open(CACHE);
        await cache.put(event.request, copy);
      }
      return response;
    } catch (error) {
      const fallback = await caches.match('./index.html');
      if (fallback) return fallback;
      throw error;
    }
  })());
});
