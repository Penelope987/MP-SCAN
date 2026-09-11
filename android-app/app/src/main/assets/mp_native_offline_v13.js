(function () {
  if (!window.MPScanApp) return;

  const clean = (value) => String(value || '').replace(/\s+/g, ' ').trim();
  const chapterRoute = () => {
    const m = (location.hash || '').match(/#\/capitulo\/([^/]+)\/([^/?]+)/);
    return m ? { work: decodeURIComponent(m[1]), chapter: decodeURIComponent(m[2]) } : null;
  };
  const workRoute = () => {
    const m = (location.hash || '').match(/#\/obra\/([^/?]+)/);
    return m ? decodeURIComponent(m[1]) : '';
  };

  function routeHash(route) {
    route = String(route || '');
    const hashAt = route.indexOf('#');
    if (hashAt >= 0) return route.slice(hashAt);
    if (route.startsWith('/')) return '#' + route;
    return '#/' + route.replace(/^\/?/, '');
  }

  function chapterLabelFromText(value) {
    const text = clean(value);
    if (!text) return '';
    let m = text.match(/(?:cap[ií]tulo|cap\.?)[\s:#-]*([0-9]+(?:[.,][0-9]+)?)/i);
    if (m) return 'Capítulo ' + m[1].replace(',', '.');
    if (/\bpr[oó]logo\b/i.test(text)) return 'Prólogo';
    if (/\bextra\b/i.test(text)) {
      m = text.match(/extra[\s:#-]*([0-9]+)?/i);
      return m && m[1] ? 'Extra ' + m[1] : 'Extra';
    }
    return '';
  }

  function findWorkTitle() {
    const info = chapterRoute();
    const workId = info ? info.work : workRoute();
    if (workId) {
      const remembered = sessionStorage.getItem('mpNativeWorkTitle:' + workId);
      if (remembered) return remembered;
    }

    const selectors = [
      '[data-work-title]', '.reader-work-title', '.reader-book-title',
      '.chapter-work-title', '.work-title', '.obra-title', '.series-title',
      '.work-detail-title', '.obra-detail-title'
    ];
    for (const selector of selectors) {
      const el = document.querySelector(selector);
      if (el && clean(el.textContent)) return clean(el.textContent);
    }

    const backLink = document.querySelector('a[href*="#/obra/"],a[href*="/obra/"]');
    if (backLink && clean(backLink.textContent) && !chapterLabelFromText(backLink.textContent)) {
      return clean(backLink.textContent);
    }

    if ((location.hash || '').startsWith('#/obra/')) {
      const heading = document.querySelector('h1,h2');
      if (heading && clean(heading.textContent)) return clean(heading.textContent);
    }
    return 'Obra MP SCAN';
  }

  function rememberWorkTitle() {
    const workId = workRoute();
    if (!workId) return;
    const title = findWorkTitle();
    if (title && title !== 'Obra MP SCAN') {
      sessionStorage.setItem('mpNativeWorkTitle:' + workId, title);
    }
  }

  function currentChapterLabel() {
    const nativeData = window.__readerPages;
    if (nativeData && nativeData.c && nativeData.c.number !== undefined && nativeData.c.number !== '') {
      return 'Capítulo ' + clean(nativeData.c.number);
    }
    const pending = sessionStorage.getItem('mpNativePendingChapterLabel');
    if (pending) return pending;
    const selectors = [
      '[data-chapter-number]', '[data-chapter-title]', '.reader-chapter-title',
      '.chapter-title', '.reader-title', '.chapter-number', '.reader-chapter-number'
    ];
    for (const selector of selectors) {
      const el = document.querySelector(selector);
      if (!el) continue;
      const byAttr = clean(el.getAttribute && (el.getAttribute('data-chapter-number') || el.getAttribute('data-chapter-title')));
      const label = chapterLabelFromText(byAttr || el.textContent);
      if (label) return label;
    }
    const headings = document.querySelectorAll('#chapterReaderContent h1,#chapterReaderContent h2,h1,h2');
    for (const h of headings) {
      const label = chapterLabelFromText(h.textContent);
      if (label) return label;
    }
    return 'Capítulo';
  }

  function currentChapterTitle() {
    const nativeData = window.__readerPages;
    if (nativeData && nativeData.c && clean(nativeData.c.title)) return clean(nativeData.c.title);
    const selectors = ['[data-chapter-title]', '.reader-chapter-title', '.chapter-title', '.reader-title'];
    for (const selector of selectors) {
      const el = document.querySelector(selector);
      if (el && clean(el.textContent)) return clean(el.textContent);
    }
    const heading = document.querySelector('#chapterReaderContent h1,#chapterReaderContent h2,h1,h2');
    const text = heading && clean(heading.textContent);
    return text || currentChapterLabel() || document.title || 'Capítulo MP SCAN';
  }

  function pageSources() {
    const selectors = [
      '#chapterReaderContent img', '#readerPages img', '.reader-pages img',
      '#readerStage img', '.reader-stage img', '.chapter-reader img',
      '.reader-content img', '.reader-images img', '.reader-view img',
      '[data-reader-pages] img'
    ];
    const images = [];
    selectors.forEach(selector => document.querySelectorAll(selector).forEach(img => images.push(img)));
    if (!images.length) {
      document.querySelectorAll('main img,article img,#app img').forEach(img => {
        const h = img.naturalHeight || img.height || 0;
        const w = img.naturalWidth || img.width || 0;
        const src = img.currentSrc || img.src || '';
        if ((h > 520 && w > 220) || src.startsWith('data:image/')) images.push(img);
      });
    }
    const seen = new Set();
    return images.map(img => img.currentSrc || img.src || '').filter(src => src && !seen.has(src) && seen.add(src));
  }

  function workMetadata() {
    const pick = selectors => { for (const s of selectors) { const e = document.querySelector(s); if (e && clean(e.textContent)) return clean(e.textContent); } return ''; };
    const image = selectors => { for (const s of selectors) { const e = document.querySelector(s); const src = e && (e.currentSrc || e.src); if (src) return src; } return ''; };
    const info = {};
    document.querySelectorAll('.work-info-cell').forEach(cell => {
      const label = clean(cell.querySelector('small')?.textContent).toLowerCase();
      const value = clean(cell.querySelector('strong')?.textContent);
      if (label && value) info[label] = value;
    });
    return {
      name: findWorkTitle(),
      altName: pick(['.work-cinema-alt','[data-work-alt-name]','.work-alt-title','.obra-subtitle']),
      synopsis: pick(['.work-synopsis-card p','[data-work-synopsis]','.work-synopsis','.obra-sinopse','.synopsis']),
      cover: image(['.work-poster img','.detail-cover img','[data-work-cover] img','.obra-cover img']),
      type: info['tipo'] || '', status: info['status'] || '', author: info['autor'] || '', artist: info['artista'] || '', scan: info['scan'] || '', language: info['idioma'] || ''
    };
  }

  function hideWholeWorkDownloads() {
    document.querySelectorAll('a,button,[role="button"]').forEach(el => {
      if (el.id === 'mp-native-downloads' || el.id === 'mp-native-chapter-download' || el.classList.contains('mp-native-ch-download')) return;
      const text = clean(el.textContent).toLowerCase();
      const whole = text.includes('baixar obra') || text.includes('baixar a obra') ||
        text.includes('download da obra') || text.includes('download obra') ||
        text.includes('baixar tudo') || text.includes('baixar todos os capítulos') ||
        text.includes('baixar todos os capitulos');
      if (whole) el.style.setProperty('display', 'none', 'important');
    });
  }

  function ensureCss() {
    if (document.getElementById('mp-native-offline-style')) return;
    const style = document.createElement('style');
    style.id = 'mp-native-offline-style';
    style.textContent = `
      .mp-native-ch-download{margin-left:8px;border:1px solid rgba(255,255,255,.13);background:linear-gradient(135deg,#713aa5,#bd4d98);color:#fff;border-radius:12px;padding:8px 10px;font:800 11px system-ui;cursor:pointer;white-space:nowrap;box-shadow:0 8px 18px rgba(0,0,0,.16)}
      .mp-native-ch-download.downloaded{background:rgba(91,56,116,.24);border-color:rgba(194,143,229,.45);color:#e9ccff;box-shadow:none}
      #mp-native-progress{position:fixed;left:50%;bottom:143px;transform:translateX(-50%);z-index:2147483640;display:none;max-width:88vw;background:rgba(17,11,24,.98);border:1px solid rgba(255,255,255,.16);color:#fff;border-radius:17px;padding:13px 17px;font:750 12px system-ui;box-shadow:0 18px 44px rgba(0,0,0,.4);text-align:center}
      .mp-native-library-work{padding:15px;border:1px solid var(--line,rgba(255,255,255,.13));border-radius:20px;background:var(--surface,#17101e);display:grid;gap:11px}.mp-native-library-work h3{margin:0;font:900 16px system-ui}.mp-native-library-chapters{display:grid;gap:8px}.mp-native-library-chapter{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:11px;border-radius:14px;background:var(--surface-2,#24172d);border:1px solid var(--line,rgba(255,255,255,.1))}.mp-native-library-chapter span{font:750 12px system-ui}.mp-native-library-open{min-height:40px;padding:0 13px;border:0;border-radius:12px;background:var(--accent,#7b4dff);color:#fff;font:900 11px system-ui}.mp-native-library-empty{grid-column:1/-1;padding:24px;text-align:center;color:var(--muted,#bdaec7)}
      @media(max-width:520px){.mp-native-ch-download{padding:7px 9px;font-size:10px}}
    `;
    document.head.appendChild(style);
  }

  function ensureProgress() {
    if (document.getElementById('mp-native-progress')) return;
    const el = document.createElement('div');
    el.id = 'mp-native-progress';
    document.body.appendChild(el);
    window.MPScanNativeUI = {
      progress(text) { el.textContent = text; el.style.display = 'block'; },
      done(text) { el.textContent = text || 'Capítulo salvo para leitura offline ✓'; el.style.display = 'block'; setTimeout(() => el.style.display = 'none', 2800); setTimeout(refresh, 120); },
      fail(text) { el.textContent = text || 'Não foi possível baixar o capítulo.'; el.style.display = 'block'; setTimeout(() => el.style.display = 'none', 3800); }
    };
  }

  function startCurrentDownload() {
    const info = chapterRoute();
    if (!info) return;
    const pages = pageSources();
    if (!pages.length) {
      window.MPScanNativeUI.fail('As páginas ainda não carregaram. Tente novamente em alguns segundos.');
      return;
    }
    const workTitle = findWorkTitle();
    const chapterLabel = currentChapterLabel();
    const chapterTitle = currentChapterTitle();
    if (workTitle && workTitle !== 'Obra MP SCAN') sessionStorage.setItem('mpNativeWorkTitle:' + info.work, workTitle);
    window.MPScanNativeUI.progress('Preparando ' + pages.length + ' páginas de ' + chapterLabel + '…');
    MPScanApp.downloadChapter(info.work, info.chapter, workTitle, chapterLabel, chapterTitle, JSON.stringify(pages), JSON.stringify(workMetadata()));
  }

  function waitAndDownload(tries) {
    tries = tries || 0;
    if (pageSources().length) return startCurrentDownload();
    if (tries > 40) return window.MPScanNativeUI.fail('Não encontrei as páginas deste capítulo.');
    setTimeout(() => waitAndDownload(tries + 1), 500);
  }

  function syncNativeDownloadsPanel() {
    if (!(location.hash || '').startsWith('#/biblioteca/downloads')) return;
    const panel = document.getElementById('libraryDownloads');
    if (!panel || panel.dataset.nativeAndroid === '1') return;
    panel.dataset.nativeAndroid = '1';
    let groups=[]; try{groups=JSON.parse(MPScanApp.offlineLibraryJson()||'[]')}catch(_e){}
    if(!groups.length){panel.innerHTML='<div class="mp-native-library-empty">Nenhum capítulo baixado neste aparelho.</div>';return;}
    panel.innerHTML=groups.map(work=>'<section class="mp-native-library-work"><h3>'+escapeHtml(work.title||'Obra MP SCAN')+'</h3><div class="mp-native-library-chapters">'+(work.chapters||[]).map(ch=>'<div class="mp-native-library-chapter"><span>'+escapeHtml(ch.label||'Capítulo')+'</span><button class="mp-native-library-open" data-offline-work="'+escapeHtml(work.key)+'" data-offline-chapter="'+escapeHtml(ch.key)+'">Ler offline</button></div>').join('')+'</div></section>').join('');
    panel.querySelectorAll('[data-offline-chapter]').forEach(button=>button.onclick=()=>MPScanApp.openStoredChapter(button.dataset.offlineWork,button.dataset.offlineChapter));
  }

  function escapeHtml(value){return String(value||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}

  function updateCurrentChapterButton() {
    const info = chapterRoute();
    let button = document.querySelector('[data-download-chapter]');
    if (!info) {
      return;
    }
    const downloaded = MPScanApp.isChapterDownloaded(info.work, info.chapter);
    if (!button) return;
    button.classList.toggle('downloaded', downloaded);
    if (downloaded) {
      button.textContent = '✓ Baixado • Ler offline';
      button.onclick = () => MPScanApp.openChapterOffline(info.work, info.chapter);
    } else {
      button.textContent = '⬇ Baixar ' + currentChapterLabel();
      button.onclick = startCurrentDownload;
    }
    button.onclick = event => { event.preventDefault(); event.stopPropagation(); event.stopImmediatePropagation(); downloaded ? MPScanApp.openChapterOffline(info.work, info.chapter) : startCurrentDownload(); };
  }

  function addWorkChapterButtons() {
    if (!(location.hash || '').startsWith('#/obra/')) return;
    rememberWorkTitle();
    const workId = workRoute();
    const workTitle = findWorkTitle();
    const used = new Set();

    document.querySelectorAll('a[href*="/capitulo/"],[data-route*="/capitulo/"],[data-chapter-id]').forEach(link => {
      let route = link.getAttribute('data-route') || link.getAttribute('href') || '';
      let hash = routeHash(route);
      if (!hash.startsWith('#/capitulo/')) {
        const chapterId = link.getAttribute('data-chapter-id');
        if (chapterId && workId) hash = '#/capitulo/' + encodeURIComponent(workId) + '/' + encodeURIComponent(chapterId);
      }
      if (!hash.startsWith('#/capitulo/') || used.has(hash)) return;
      used.add(hash);

      const match = hash.match(/#\/capitulo\/([^/]+)\/([^/?]+)/);
      if (!match) return;
      const targetWork = decodeURIComponent(match[1]);
      const targetChapter = decodeURIComponent(match[2]);
      const row = link.closest('li,article,.chapter-item,.chapter-row,.chapter-card,.episode-item') || link.parentElement || link;
      const rowText = clean(row.textContent || link.textContent);
      const chapterLabel = chapterLabelFromText(rowText) || chapterLabelFromText(link.textContent) || 'Capítulo';
      const downloaded = MPScanApp.isChapterDownloaded(targetWork, targetChapter);

      let button = row.querySelector && row.querySelector('.mp-native-ch-download[data-target="' + CSS.escape(hash) + '"]');
      if (!button) {
        button = document.createElement('button');
        button.type = 'button';
        button.className = 'mp-native-ch-download';
        button.setAttribute('data-target', hash);
        if (row && row.appendChild) row.appendChild(button);
      }
      button.classList.toggle('downloaded', downloaded);
      button.textContent = downloaded ? '✓ ' + chapterLabel + ' baixado' : '⬇ Baixar ' + chapterLabel;
      button.onclick = event => {
        event.preventDefault();
        event.stopPropagation();
        if (downloaded) {
          MPScanApp.openChapterOffline(targetWork, targetChapter);
          return;
        }
        if (workId && workTitle) sessionStorage.setItem('mpNativeWorkTitle:' + workId, workTitle);
        sessionStorage.setItem('mpNativePendingChapterLabel', chapterLabel);
        sessionStorage.setItem('mpNativePendingDownload', hash);
        location.hash = hash;
      };
    });
  }

  function checkPendingDownload() {
    const pending = sessionStorage.getItem('mpNativePendingDownload');
    if (pending && pending === location.hash && chapterRoute()) {
      sessionStorage.removeItem('mpNativePendingDownload');
      setTimeout(() => waitAndDownload(0), 700);
    }
  }

  function refresh() {
    const promo=document.getElementById('downloadAndroidAppCard');if(promo)promo.style.setProperty('display','none','important');
    hideWholeWorkDownloads();
    rememberWorkTitle();
    syncNativeDownloadsPanel();
    updateCurrentChapterButton();
    addWorkChapterButtons();
    checkPendingDownload();
  }

  if (!window.__mpNativeOfflineInstalledV13) {
    window.__mpNativeOfflineInstalledV13 = true;
    ensureCss();
    ensureProgress();
    window.addEventListener('hashchange', () => setTimeout(refresh, 130));
    new MutationObserver(() => {
      clearTimeout(window.__mpNativeRefreshTimerV13);
      window.__mpNativeRefreshTimerV13 = setTimeout(refresh, 220);
    }).observe(document.documentElement, { childList: true, subtree: true });
  }

  window.MPScanNativeRefresh = refresh;
  refresh();
})();
