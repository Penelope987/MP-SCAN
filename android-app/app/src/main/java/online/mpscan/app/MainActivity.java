package online.mpscan.app;

import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends ComponentActivity {
    private static final String HOME = "https://www.mpscan.online/";
    private static final String OFFLINE_DIR = "mp_scan_offline";

    private WebView web;
    private View loadingOverlay;
    private ValueCallback<Uri[]> fileCallback;
    private ActivityResultLauncher<Intent> fileLauncher;
    private String nativeScreen = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(13, 9, 18));
        getWindow().setNavigationBarColor(Color.rgb(13, 9, 18));

        fileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Uri[] out = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        out = new Uri[n];
                        for (int i = 0; i < n; i++) {
                            out[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        out = new Uri[]{data.getData()};
                    }
                }
                if (fileCallback != null) fileCallback.onReceiveValue(out);
                fileCallback = null;
            }
        );

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(40, 40, 40, 40);
        loading.setBackgroundColor(Color.rgb(13, 9, 18));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.mp_scan_icon);
        loading.addView(logo, new LinearLayout.LayoutParams(150, 150));

        TextView title = new TextView(this);
        title.setText("MP SCAN");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 22, 0, 16);
        loading.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Preparando sua leitura…");
        subtitle.setTextColor(Color.rgb(194, 181, 208));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 18);
        loading.addView(subtitle);

        ProgressBar bar = new ProgressBar(this);
        loading.addView(bar);
        loadingOverlay = loading;
        root.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new OfflineBridge(), "MPScanApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                nativeScreen = "";
                hideLoading();
                injectNativeFeatures();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && hasOfflineDownloads()) {
                    showOfflineLibrary();
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileLauncher.launch(intent);
                return true;
            }
        });

        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                );
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(HOME);
        }
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) return false;

        if ("mpscan-offline".equals(uri.getScheme())) {
            String host = uri.getHost() == null ? "" : uri.getHost();

            if ("library".equals(host)) {
                showOfflineLibrary();
                return true;
            }

            if ("online".equals(host)) {
                nativeScreen = "";
                web.loadUrl(HOME);
                return true;
            }

            List<String> seg = uri.getPathSegments();
            if ("read".equals(host) && seg.size() >= 2) {
                showOfflineReader(seg.get(0), seg.get(1));
                return true;
            }

            if ("delete".equals(host) && seg.size() >= 2) {
                deleteOfflineChapter(seg.get(0), seg.get(1));
                showOfflineLibrary();
                return true;
            }
            return true;
        }

        String host = uri.getHost() == null ? "" : uri.getHost();
        if (host.endsWith("mpscan.online") || host.endsWith("firebaseapp.com") || host.endsWith("googleapis.com")) {
            return false;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {}
        return true;
    }

    private void hideLoading() {
        if (loadingOverlay == null || loadingOverlay.getVisibility() == View.GONE) return;
        loadingOverlay.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
            .start();
    }

    private File offlineRoot() {
        File root = new File(getFilesDir(), OFFLINE_DIR);
        if (!root.exists()) root.mkdirs();
        return root;
    }

    private String safeKey(String raw) {
        if (raw == null) raw = "";
        return Base64.encodeToString(
            raw.getBytes(StandardCharsets.UTF_8),
            Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
    }

    private File chapterDir(String workId, String chapterId) {
        return new File(new File(offlineRoot(), safeKey(workId)), safeKey(chapterId));
    }

    private boolean hasOfflineDownloads() {
        return !listOfflineChapters().isEmpty();
    }

    private boolean isDownloaded(String workId, String chapterId) {
        return new File(chapterDir(workId, chapterId), "meta.json").exists();
    }

    private void injectNativeFeatures() {
        String js =
            "(function(){"
            + "if(!window.MPScanApp){return;}"
            + "if(window.__mpNativeOfflineInstalled){if(window.MPScanNativeRefresh)window.MPScanNativeRefresh();return;}"
            + "window.__mpNativeOfflineInstalled=true;"
            + "var css=document.createElement('style');css.id='mp-native-offline-style';"
            + "css.textContent='"
            + "#mp-native-downloads{position:fixed;right:14px;bottom:82px;z-index:2147483000;border:1px solid rgba(255,255,255,.16);background:linear-gradient(135deg,#8f4ac7,#d85aa7);color:#fff;border-radius:999px;padding:10px 14px;font:800 12px system-ui;box-shadow:0 12px 30px rgba(0,0,0,.28);cursor:pointer}'"
            + "+'#mp-native-chapter-download{position:fixed;left:14px;bottom:82px;z-index:2147483000;border:1px solid rgba(255,255,255,.16);background:rgba(22,15,31,.95);backdrop-filter:blur(14px);color:#fff;border-radius:999px;padding:10px 14px;font:800 12px system-ui;box-shadow:0 12px 30px rgba(0,0,0,.28);cursor:pointer}'"
            + "+'.mp-native-ch-download{margin-left:8px;border:0;background:linear-gradient(135deg,#8f4ac7,#d85aa7);color:#fff;border-radius:10px;padding:7px 10px;font:800 11px system-ui;cursor:pointer;white-space:nowrap}'"
            + "+'#mp-native-progress{position:fixed;left:50%;bottom:140px;transform:translateX(-50%);z-index:2147483640;display:none;max-width:86vw;background:rgba(18,12,27,.97);border:1px solid rgba(255,255,255,.15);color:#fff;border-radius:16px;padding:12px 16px;font:700 12px system-ui;box-shadow:0 18px 40px rgba(0,0,0,.35);text-align:center}'"
            + "+'@media(max-width:520px){#mp-native-downloads,#mp-native-chapter-download{bottom:76px;padding:9px 12px;font-size:11px}.mp-native-ch-download{padding:6px 8px;font-size:10px}}';"
            + "document.head.appendChild(css);"
            + "var p=document.createElement('div');p.id='mp-native-progress';document.body.appendChild(p);"
            + "window.MPScanNativeUI={"
            + "progress:function(t){var e=document.getElementById('mp-native-progress');if(!e)return;e.textContent=t;e.style.display='block';},"
            + "done:function(t){var e=document.getElementById('mp-native-progress');if(!e)return;e.textContent=t||'Capítulo salvo para leitura offline ✓';setTimeout(function(){e.style.display='none';},2600);window.MPScanNativeRefresh&&window.MPScanNativeRefresh();},"
            + "fail:function(t){var e=document.getElementById('mp-native-progress');if(!e)return;e.textContent=t||'Não foi possível baixar o capítulo.';e.style.display='block';setTimeout(function(){e.style.display='none';},3600);}"
            + "};"
            + "function hashFor(route){route=route||'';var i=route.indexOf('#');if(i>=0)return route.slice(i);if(route.charAt(0)==='/')return '#'+route;return '#/'+route.replace(/^\\/?/,'');}"
            + "function chapterInfo(){var m=(location.hash||'').match(/#\\/capitulo\\/([^\\/]+)\\/([^\\/?]+)/);return m?{work:decodeURIComponent(m[1]),chapter:decodeURIComponent(m[2])}:null;}"
            + "function pageSources(){"
            + "var selectors=['#readerPages img','.reader-pages img','#readerStage img','.reader-stage img','.chapter-reader img','.reader-content img','.reader-images img','.reader-view img','[data-reader-pages] img'];"
            + "var imgs=[];selectors.forEach(function(s){document.querySelectorAll(s).forEach(function(i){imgs.push(i);});});"
            + "if(!imgs.length){document.querySelectorAll('main img,article img,#app img').forEach(function(i){var h=i.naturalHeight||i.height||0,w=i.naturalWidth||i.width||0,src=i.currentSrc||i.src||'';if((h>520&&w>220)||src.indexOf('data:image/')===0)imgs.push(i);});}"
            + "var seen={},out=[];imgs.forEach(function(i){var s=i.currentSrc||i.src||'';if(!s||seen[s])return;seen[s]=1;out.push(s);});return out;"
            + "}"
            + "function chapterTitle(){var el=document.querySelector('.reader-title,.chapter-title,[data-chapter-title],h1,h2');var t=el&&el.textContent?el.textContent.trim():'';return t||document.title||'Capítulo MP SCAN';}"
            + "function downloadNow(){var info=chapterInfo();if(!info)return;var pages=pageSources();if(!pages.length){window.MPScanNativeUI.fail('As páginas ainda não carregaram. Tente novamente em alguns segundos.');return;}window.MPScanNativeUI.progress('Preparando '+pages.length+' páginas…');MPScanApp.downloadChapter(info.work,info.chapter,chapterTitle(),JSON.stringify(pages));}"
            + "function waitAndDownload(tries){tries=tries||0;var pages=pageSources();if(pages.length){downloadNow();return;}if(tries>40){window.MPScanNativeUI.fail('Não encontrei as páginas deste capítulo.');return;}setTimeout(function(){waitAndDownload(tries+1);},500);}"
            + "function addGlobalButton(){var b=document.getElementById('mp-native-downloads');if(!b){b=document.createElement('button');b.id='mp-native-downloads';b.type='button';b.textContent='📚 Downloads';b.onclick=function(){MPScanApp.openDownloads();};document.body.appendChild(b);}}"
            + "function addChapterButton(){var info=chapterInfo(),b=document.getElementById('mp-native-chapter-download');if(!info){if(b)b.remove();return;}if(!b){b=document.createElement('button');b.id='mp-native-chapter-download';b.type='button';b.onclick=downloadNow;document.body.appendChild(b);}b.textContent=MPScanApp.isChapterDownloaded(info.work,info.chapter)?'✓ Baixado':'⬇ Baixar capítulo';}"
            + "function addWorkButtons(){if((location.hash||'').indexOf('#/obra/')!==0)return;var used={};document.querySelectorAll('a[href*=\"/capitulo/\"],[data-route*=\"/capitulo/\"]').forEach(function(a){var route=a.getAttribute('data-route')||a.getAttribute('href')||'';var h=hashFor(route);if(h.indexOf('#/capitulo/')!==0||used[h])return;used[h]=1;var parent=a.parentElement||a;var exists=false;if(parent.querySelectorAll){parent.querySelectorAll('.mp-native-ch-download').forEach(function(x){if(x.getAttribute('data-target')===h)exists=true;});}if(exists)return;var b=document.createElement('button');b.type='button';b.className='mp-native-ch-download';b.setAttribute('data-target',h);var m=h.match(/#\\/capitulo\\/([^\\/]+)\\/([^\\/?]+)/);var downloaded=false;if(m){try{downloaded=MPScanApp.isChapterDownloaded(decodeURIComponent(m[1]),decodeURIComponent(m[2]));}catch(e){}}b.textContent=downloaded?'✓ Offline':'⬇ Baixar';b.onclick=function(ev){ev.preventDefault();ev.stopPropagation();sessionStorage.setItem('mpNativePendingDownload',h);location.hash=h;};parent.appendChild(b);});}"
            + "function checkPending(){var p=sessionStorage.getItem('mpNativePendingDownload');if(p&&p===location.hash&&chapterInfo()){sessionStorage.removeItem('mpNativePendingDownload');setTimeout(function(){waitAndDownload(0);},700);}}"
            + "window.MPScanNativeRefresh=function(){addGlobalButton();addChapterButton();addWorkButtons();checkPending();};"
            + "window.addEventListener('hashchange',function(){setTimeout(window.MPScanNativeRefresh,120);});"
            + "new MutationObserver(function(){clearTimeout(window.__mpNativeRefreshTimer);window.__mpNativeRefreshTimer=setTimeout(window.MPScanNativeRefresh,180);}).observe(document.documentElement,{childList:true,subtree:true});"
            + "window.MPScanNativeRefresh();"
            + "})();";
        web.evaluateJavascript(js, null);
    }

    private void sendJsProgress(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() ->
            web.evaluateJavascript(
                "window.MPScanNativeUI&&window.MPScanNativeUI.progress(" + safe + ");",
                null
            )
        );
    }

    private void sendJsDone(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() ->
            web.evaluateJavascript(
                "window.MPScanNativeUI&&window.MPScanNativeUI.done(" + safe + ");",
                null
            )
        );
    }

    private void sendJsFail(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() ->
            web.evaluateJavascript(
                "window.MPScanNativeUI&&window.MPScanNativeUI.fail(" + safe + ");",
                null
            )
        );
    }

    private class OfflineBridge {
        @JavascriptInterface
        public void openDownloads() {
            runOnUiThread(MainActivity.this::showOfflineLibrary);
        }

        @JavascriptInterface
        public boolean isChapterDownloaded(String workId, String chapterId) {
            return isDownloaded(workId, chapterId);
        }

        @JavascriptInterface
        public void downloadChapter(String workId, String chapterId, String title, String pagesJson) {
            new Thread(() -> downloadChapterInternal(workId, chapterId, title, pagesJson)).start();
        }
    }

    private void downloadChapterInternal(String workId, String chapterId, String title, String pagesJson) {
        File tmp = null;
        try {
            JSONArray pages = new JSONArray(pagesJson);
            if (pages.length() == 0) {
                sendJsFail("Este capítulo não possui páginas para baixar.");
                return;
            }

            File finalDir = chapterDir(workId, chapterId);
            File parent = finalDir.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            tmp = new File(parent, finalDir.getName() + "_tmp_" + System.currentTimeMillis());
            deleteRecursive(tmp);
            if (!tmp.mkdirs()) throw new Exception("Não foi possível preparar a pasta offline.");

            JSONArray savedFiles = new JSONArray();

            for (int i = 0; i < pages.length(); i++) {
                String src = pages.optString(i, "");
                if (src.isEmpty()) continue;

                sendJsProgress("Baixando página " + (i + 1) + " de " + pages.length() + "…");

                PageData page = readPage(src);
                String name = String.format("%04d%s", i + 1, page.extension);
                File out = new File(tmp, name);

                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                    os.write(page.bytes);
                }

                savedFiles.put(name);
            }

            if (savedFiles.length() == 0) {
                throw new Exception("Nenhuma página pôde ser salva.");
            }

            JSONObject meta = new JSONObject();
            meta.put("workId", workId);
            meta.put("chapterId", chapterId);
            meta.put("title", title == null || title.trim().isEmpty() ? "Capítulo MP SCAN" : title.trim());
            meta.put("pageCount", savedFiles.length());
            meta.put("savedAt", System.currentTimeMillis());
            meta.put("files", savedFiles);

            try (OutputStream os = new FileOutputStream(new File(tmp, "meta.json"))) {
                os.write(meta.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (finalDir.exists()) deleteRecursive(finalDir);
            if (!tmp.renameTo(finalDir)) {
                copyDirectory(tmp, finalDir);
                deleteRecursive(tmp);
            }

            sendJsDone("Capítulo baixado! Agora ele pode ser lido sem internet. ✓");
        } catch (Exception e) {
            if (tmp != null) deleteRecursive(tmp);
            sendJsFail("Erro ao baixar: " + cleanError(e));
        }
    }

    private static class PageData {
        final byte[] bytes;
        final String extension;

        PageData(byte[] bytes, String extension) {
            this.bytes = bytes;
            this.extension = extension;
        }
    }

    private PageData readPage(String src) throws Exception {
        if (src.startsWith("data:image/")) {
            int comma = src.indexOf(',');
            if (comma < 0) throw new Exception("Imagem inválida.");
            String head = src.substring(0, comma);
            String mime = head.substring(5).split(";")[0];
            String body = src.substring(comma + 1);
            byte[] bytes;
            if (head.contains(";base64")) {
                bytes = Base64.decode(body, Base64.DEFAULT);
            } else {
                bytes = Uri.decode(body).getBytes(StandardCharsets.ISO_8859_1);
            }
            return new PageData(bytes, extForMime(mime));
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(src).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", web.getSettings().getUserAgentString());

        String cookie = CookieManager.getInstance().getCookie(src);
        if (cookie != null && !cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 400) {
            conn.disconnect();
            throw new Exception("Servidor respondeu " + code + ".");
        }

        String mime = conn.getContentType();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(conn.getInputStream())) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = is.read(buffer)) != -1) bos.write(buffer, 0, read);
        } finally {
            conn.disconnect();
        }

        return new PageData(bos.toByteArray(), extForMime(mime));
    }

    private String extForMime(String mime) {
        if (mime == null) return ".jpg";
        mime = mime.toLowerCase();
        if (mime.contains("png")) return ".png";
        if (mime.contains("webp")) return ".webp";
        if (mime.contains("gif")) return ".gif";
        if (mime.contains("avif")) return ".avif";
        return ".jpg";
    }

    private String cleanError(Exception e) {
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) return "falha inesperada";
        if (m.length() > 120) return m.substring(0, 120);
        return m;
    }

    private void copyDirectory(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) throw new Exception("Não foi possível concluir o download.");
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyDirectory(child, new File(dst, child.getName()));
                }
            }
            return;
        }

        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
    }

    private static class OfflineItem {
        final File workDir;
        final File chapterDir;
        final JSONObject meta;

        OfflineItem(File workDir, File chapterDir, JSONObject meta) {
            this.workDir = workDir;
            this.chapterDir = chapterDir;
            this.meta = meta;
        }
    }

    private List<OfflineItem> listOfflineChapters() {
        List<OfflineItem> items = new ArrayList<>();
        File root = offlineRoot();
        File[] works = root.listFiles(File::isDirectory);
        if (works == null) return items;

        for (File workDir : works) {
            File[] chapters = workDir.listFiles(File::isDirectory);
            if (chapters == null) continue;

            for (File ch : chapters) {
                File metaFile = new File(ch, "meta.json");
                if (!metaFile.exists()) continue;
                try {
                    String raw = readText(metaFile);
                    JSONObject meta = new JSONObject(raw);
                    items.add(new OfflineItem(workDir, ch, meta));
                } catch (Exception ignored) {}
            }
        }

        Collections.sort(items, (a, b) ->
            Long.compare(b.meta.optLong("savedAt", 0), a.meta.optLong("savedAt", 0))
        );
        return items;
    }

    private String readText(File file) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) bos.write(buffer, 0, n);
        }
        return bos.toString("UTF-8");
    }

    private String html(String s) {
        if (s == null) return "";
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private void showOfflineLibrary() {
        nativeScreen = "library";
        List<OfflineItem> items = listOfflineChapters();

        StringBuilder cards = new StringBuilder();
        int totalPages = 0;

        for (OfflineItem item : items) {
            JSONObject m = item.meta;
            int pages = m.optInt("pageCount", 0);
            totalPages += pages;
            String title = m.optString("title", "Capítulo MP SCAN");
            long savedAt = m.optLong("savedAt", 0);
            String date = savedAt > 0
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(savedAt))
                : "";

            String workKey = Uri.encode(item.workDir.getName());
            String chapterKey = Uri.encode(item.chapterDir.getName());

            cards.append("<article class='card'>")
                .append("<div class='icon'>📖</div>")
                .append("<div class='copy'><strong>").append(html(title)).append("</strong>")
                .append("<span>").append(pages).append(" páginas")
                .append(date.isEmpty() ? "" : " • salvo em " + html(date))
                .append("</span></div>")
                .append("<div class='actions'>")
                .append("<a class='read' href='mpscan-offline://read/").append(workKey).append("/").append(chapterKey).append("'>Ler offline</a>")
                .append("<a class='del' href='mpscan-offline://delete/").append(workKey).append("/").append(chapterKey).append("'>Excluir</a>")
                .append("</div></article>");
        }

        String empty = items.isEmpty()
            ? "<div class='empty'><div>📥</div><h2>Nenhum capítulo baixado</h2><p>Abra uma obra ou capítulo no aplicativo e toque em <b>Baixar</b>. Depois ele aparecerá aqui.</p></div>"
            : cards.toString();

        String page =
            "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'>"
            + "<style>"
            + "*{box-sizing:border-box}body{margin:0;background:#0d0912;color:#fff;font-family:system-ui,-apple-system,sans-serif;padding:22px 16px 40px}"
            + ".top{max-width:780px;margin:0 auto 20px}.eyebrow{color:#d893ff;font-weight:900;font-size:12px;letter-spacing:.12em}.top h1{font-size:30px;margin:6px 0 6px}.top p{color:#bcaecb;margin:0;line-height:1.45}"
            + ".stats{display:flex;gap:10px;margin-top:15px}.stat{background:#181020;border:1px solid #30223d;border-radius:16px;padding:12px 14px;min-width:110px}.stat b{display:block;font-size:20px}.stat span{color:#a995b9;font-size:12px}"
            + ".list{max-width:780px;margin:auto;display:grid;gap:12px}.card{display:grid;grid-template-columns:52px minmax(0,1fr);gap:12px;background:linear-gradient(145deg,#1a1124,#130d1b);border:1px solid #342342;border-radius:22px;padding:14px;box-shadow:0 15px 35px rgba(0,0,0,.22)}"
            + ".icon{width:52px;height:52px;border-radius:16px;display:grid;place-items:center;background:linear-gradient(135deg,#8f4ac7,#d85aa7);font-size:23px}.copy{min-width:0}.copy strong{display:block;font-size:15px;line-height:1.3}.copy span{display:block;color:#a995b9;font-size:12px;margin-top:5px}"
            + ".actions{grid-column:1/-1;display:flex;gap:8px}.actions a{text-decoration:none;text-align:center;flex:1;border-radius:13px;padding:10px 12px;font-weight:900;font-size:12px}.read{background:linear-gradient(135deg,#8f4ac7,#d85aa7);color:#fff}.del{background:#21172b;color:#d7c6e2;border:1px solid #3a2948}"
            + ".back{display:inline-flex;margin-top:16px;text-decoration:none;color:#fff;background:#1c1325;border:1px solid #392747;padding:10px 14px;border-radius:14px;font-weight:800;font-size:12px}.empty{max-width:580px;margin:60px auto;text-align:center;background:#17101f;border:1px solid #30223d;border-radius:24px;padding:30px 20px}.empty>div{font-size:42px}.empty p{color:#a995b9;line-height:1.5}"
            + "</style></head><body>"
            + "<section class='top'><div class='eyebrow'>MP SCAN • APLICATIVO</div><h1>Leitura offline</h1><p>Capítulos salvos ficam no próprio celular e podem ser lidos mesmo sem internet.</p>"
            + "<div class='stats'><div class='stat'><b>" + items.size() + "</b><span>capítulos</span></div><div class='stat'><b>" + totalPages + "</b><span>páginas</span></div></div>"
            + "<a class='back' href='mpscan-offline://online'>← Voltar para o MP SCAN</a></section>"
            + "<main class='list'>" + empty + "</main></body></html>";

        web.loadDataWithBaseURL("https://app.mpscan.local/", page, "text/html", "UTF-8", null);
        hideLoading();
    }

    private void showOfflineReader(String workKey, String chapterKey) {
        File ch = new File(new File(offlineRoot(), workKey), chapterKey);
        File metaFile = new File(ch, "meta.json");

        if (!metaFile.exists()) {
            showOfflineLibrary();
            return;
        }

        try {
            JSONObject meta = new JSONObject(readText(metaFile));
            JSONArray files = meta.optJSONArray("files");
            if (files == null || files.length() == 0) {
                showOfflineLibrary();
                return;
            }

            nativeScreen = "reader";
            StringBuilder imgs = new StringBuilder();
            for (int i = 0; i < files.length(); i++) {
                String name = files.optString(i, "");
                if (name.isEmpty()) continue;
                imgs.append("<img loading='eager' src='").append(html(name)).append("' alt='Página ").append(i + 1).append("'>");
            }

            String title = meta.optString("title", "Capítulo MP SCAN");

            String page =
                "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover,user-scalable=yes'>"
                + "<style>*{box-sizing:border-box}html,body{margin:0;background:#09070c;color:#fff;font-family:system-ui,-apple-system,sans-serif}.bar{position:sticky;top:0;z-index:5;display:flex;align-items:center;gap:10px;padding:12px 14px;background:rgba(13,9,18,.94);backdrop-filter:blur(14px);border-bottom:1px solid #2c2035}.bar a{color:#fff;text-decoration:none;background:#1f1528;border:1px solid #3a2948;border-radius:12px;padding:9px 11px;font-weight:900;font-size:12px}.bar div{min-width:0}.bar b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.bar span{font-size:11px;color:#aa98b7}.pages{width:100%;margin:0 auto}.pages img{display:block;width:100%;max-width:100%;height:auto;margin:0 auto;background:#130d18}</style>"
                + "</head><body><header class='bar'><a href='mpscan-offline://library'>← Downloads</a><div><b>" + html(title) + "</b><span>Modo offline • " + files.length() + " páginas</span></div></header><main class='pages'>" + imgs + "</main></body></html>";

            String base = Uri.fromFile(ch).toString();
            if (!base.endsWith("/")) base += "/";
            web.loadDataWithBaseURL(base, page, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir este download.", Toast.LENGTH_SHORT).show();
            showOfflineLibrary();
        }
    }

    private void deleteOfflineChapter(String workKey, String chapterKey) {
        File ch = new File(new File(offlineRoot(), workKey), chapterKey);
        deleteRecursive(ch);
        File work = ch.getParentFile();
        if (work != null) {
            File[] remaining = work.listFiles();
            if (remaining == null || remaining.length == 0) work.delete();
        }
        Toast.makeText(this, "Download removido", Toast.LENGTH_SHORT).show();
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        web.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if ("reader".equals(nativeScreen)) {
            showOfflineLibrary();
            return;
        }
        if ("library".equals(nativeScreen)) {
            nativeScreen = "";
            web.loadUrl(HOME);
            return;
        }
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
