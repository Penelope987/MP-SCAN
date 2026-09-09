package online.mpscan.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivityV3 extends ComponentActivity {
    private static final String HOME = "https://www.mpscan.online/";
    private static final String APP_HOME = "https://www.mpscan.online/app/";
    private static final String OFFLINE_DIR = "mp_scan_offline";
    private static final String NOTIFICATION_CHANNEL = "mp_scan_updates";

    private WebView web;
    private View loadingOverlay;
    private ValueCallback<Uri[]> fileCallback;
    private ActivityResultLauncher<Intent> fileLauncher;
    private String nativeScreen = "";
    private String nativeWorkKey = "";

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
                        for (int i = 0; i < n; i++) out[i] = data.getClipData().getItemAt(i).getUri();
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
        title.setPadding(0, 22, 0, 12);
        loading.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Organizando suas leituras…");
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
        createNotificationChannel();

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
                if (url != null && (url.startsWith("https://app.mpscan.local/") || url.startsWith(APP_HOME))) {
                    hideLoading();
                    return;
                }
                nativeScreen = "";
                nativeWorkKey = "";
                hideLoading();
                injectNativeFeatures();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivityV3.this, "O site não respondeu. Abrimos sua biblioteca offline.", Toast.LENGTH_SHORT).show();
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
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else showNativeApp();
    }

    private boolean handleUri(Uri uri) {
        if (uri == null) return false;
        if ("mpscan-offline".equals(uri.getScheme())) {
            String host = uri.getHost() == null ? "" : uri.getHost();
            List<String> seg = uri.getPathSegments();

            if ("library".equals(host)) {
                showOfflineLibrary();
                return true;
            }
            if ("history".equals(host)) {
                showHistory();
                return true;
            }
            if ("more".equals(host)) {
                showMore();
                return true;
            }
            if ("notifications".equals(host)) {
                requestNotificationPermission();
                return true;
            }
            if ("work".equals(host) && seg.size() >= 1) {
                showOfflineWork(seg.get(0));
                return true;
            }
            if ("online".equals(host)) {
                showNativeApp();
                return true;
            }
            if ("read".equals(host) && seg.size() >= 2) {
                showOfflineReader(seg.get(0), seg.get(1));
                return true;
            }
            if ("delete".equals(host) && seg.size() >= 2) {
                String workKey = seg.get(0);
                deleteOfflineChapter(workKey, seg.get(1));
                if (findGroup(workKey) != null) showOfflineWork(workKey);
                else showOfflineLibrary();
                return true;
            }
            return true;
        }

        String host = uri.getHost() == null ? "" : uri.getHost();
        if (host.endsWith("mpscan.online") || host.endsWith("firebaseapp.com") || host.endsWith("googleapis.com")) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
        return true;
    }

    private void hideLoading() {
        if (loadingOverlay == null || loadingOverlay.getVisibility() == View.GONE) return;
        loadingOverlay.animate().alpha(0f).setDuration(250)
            .withEndAction(() -> loadingOverlay.setVisibility(View.GONE)).start();
    }

    private void showNativeApp() {
        nativeScreen = "online";
        nativeWorkKey = "";
        try {
            web.loadUrl(HOME);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir a tela inicial.", Toast.LENGTH_SHORT).show();
            showOfflineLibrary();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
            NOTIFICATION_CHANNEL,
            "Atualizações do MP SCAN",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Novos capítulos, respostas e avisos importantes.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1407);
        } else {
            Toast.makeText(this, "Notificações já estão permitidas.", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyDownloadReady(String workTitle, String chapterLabel) {
        if (!notificationsAllowed()) return;
        Intent open = new Intent(this, MainActivityV3.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.mp_scan_icon)
            .setContentTitle("Capítulo disponível offline")
            .setContentText(emptyTo(workTitle, "MP SCAN") + " • " + emptyTo(chapterLabel, "Capítulo"))
            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT);
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify((workTitle + chapterLabel).hashCode(), notification.build());
    }

    private File offlineRoot() {
        File root = new File(getFilesDir(), OFFLINE_DIR);
        if (!root.exists()) root.mkdirs();
        return root;
    }

    private String safeKey(String raw) {
        if (raw == null) raw = "";
        return Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
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
        try {
            String js = readAsset("mp_native_offline_v13.js");
            web.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível carregar os controles offline.", Toast.LENGTH_SHORT).show();
        }
    }

    private String readAsset(String name) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream is = getAssets().open(name)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) bos.write(buffer, 0, n);
        }
        return bos.toString("UTF-8");
    }

    private void sendJsProgress(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() -> web.evaluateJavascript("window.MPScanNativeUI&&window.MPScanNativeUI.progress(" + safe + ");", null));
    }

    private void sendJsDone(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() -> web.evaluateJavascript("window.MPScanNativeUI&&window.MPScanNativeUI.done(" + safe + ");", null));
    }

    private void sendJsFail(String message) {
        final String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() -> web.evaluateJavascript("window.MPScanNativeUI&&window.MPScanNativeUI.fail(" + safe + ");", null));
    }

    private class OfflineBridge {
        @JavascriptInterface
        public void openDownloads() {
            runOnUiThread(MainActivityV3.this::showOfflineLibrary);
        }

        @JavascriptInterface
        public boolean isChapterDownloaded(String workId, String chapterId) {
            return isDownloaded(workId, chapterId);
        }

        @JavascriptInterface
        public int downloadedChapterCount() {
            return listOfflineChapters().size();
        }

        @JavascriptInterface
        public void openChapterOffline(String workId, String chapterId) {
            runOnUiThread(() -> {
                File dir = chapterDir(workId, chapterId);
                if (new File(dir, "meta.json").exists()) showOfflineReader(dir.getParentFile().getName(), dir.getName());
                else Toast.makeText(MainActivityV3.this, "Esse capítulo ainda não foi baixado.", Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void downloadChapter(String workId, String chapterId, String workTitle, String chapterLabel, String chapterTitle, String pagesJson, String workMetaJson) {
            new Thread(() -> downloadChapterInternal(workId, chapterId, workTitle, chapterLabel, chapterTitle, pagesJson, workMetaJson)).start();
        }
    }

    private synchronized void downloadChapterInternal(String workId, String chapterId, String workTitle, String chapterLabel, String chapterTitle, String pagesJson, String workMetaJson) {
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
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) { os.write(page.bytes); }
                savedFiles.put(name);
            }
            if (savedFiles.length() == 0) throw new Exception("Nenhuma página pôde ser salva.");

            JSONObject workMeta;
            try { workMeta = new JSONObject(workMetaJson == null ? "{}" : workMetaJson); } catch (Exception ignored) { workMeta = new JSONObject(); }
            String coverFile = "";
            String coverUrl = workMeta.optString("cover", "");
            if (!coverUrl.isEmpty()) {
                try {
                    PageData cover = readPage(coverUrl);
                    coverFile = "cover" + cover.extension;
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(tmp, coverFile)))) { os.write(cover.bytes); }
                } catch (Exception ignored) {}
            }

            String normalizedLabel = normalizeChapterLabel(chapterLabel, chapterTitle, chapterId);
            JSONObject meta = new JSONObject();
            meta.put("workId", workId);
            meta.put("chapterId", chapterId);
            meta.put("workTitle", emptyTo(workTitle, "Obra MP SCAN"));
            meta.put("chapterLabel", normalizedLabel);
            meta.put("title", emptyTo(chapterTitle, normalizedLabel));
            meta.put("workAltName", workMeta.optString("altName", ""));
            meta.put("workSynopsis", workMeta.optString("synopsis", ""));
            meta.put("workCover", coverFile);
            meta.put("workType", workMeta.optString("type", ""));
            meta.put("workStatus", workMeta.optString("status", ""));
            meta.put("workAuthor", workMeta.optString("author", ""));
            meta.put("workArtist", workMeta.optString("artist", ""));
            meta.put("workScan", workMeta.optString("scan", ""));
            meta.put("workLanguage", workMeta.optString("language", ""));
            meta.put("pageCount", savedFiles.length());
            meta.put("savedAt", System.currentTimeMillis());
            meta.put("files", savedFiles);

            try (OutputStream os = new FileOutputStream(new File(tmp, "meta.json"))) {
                os.write(meta.toString().getBytes(StandardCharsets.UTF_8));
            }

            File backupDir = new File(parent, finalDir.getName() + "_backup");
            deleteRecursive(backupDir);
            if (finalDir.exists() && !finalDir.renameTo(backupDir)) {
                throw new Exception("Não foi possível preservar o download anterior.");
            }
            try {
                if (!tmp.renameTo(finalDir)) {
                    copyDirectory(tmp, finalDir);
                    if (!new File(finalDir, "meta.json").exists()) {
                        throw new Exception("O download não foi concluído corretamente.");
                    }
                    deleteRecursive(tmp);
                }
                deleteRecursive(backupDir);
            } catch (Exception installError) {
                deleteRecursive(finalDir);
                if (backupDir.exists() && !backupDir.renameTo(finalDir)) {
                    throw new Exception("Falha ao restaurar o download anterior.");
                }
                throw installError;
            }
            sendJsDone(normalizedLabel + " baixado! Agora ele aparece dentro da página da obra. ✓");
            notifyDownloadReady(workTitle, normalizedLabel);
        } catch (Exception e) {
            if (tmp != null) deleteRecursive(tmp);
            sendJsFail("Erro ao baixar: " + cleanError(e));
        }
    }

    private String emptyTo(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String normalizeChapterLabel(String label, String title, String chapterId) {
        String[] candidates = {label, title};
        Pattern p = Pattern.compile("(?:cap[ií]tulo|cap\\.?)[\\s:#-]*([0-9]+(?:[.,][0-9]+)?)", Pattern.CASE_INSENSITIVE);
        for (String candidate : candidates) {
            if (candidate == null) continue;
            Matcher m = p.matcher(candidate);
            if (m.find()) return "Capítulo " + m.group(1).replace(',', '.');
            if (candidate.toLowerCase().contains("prólogo") || candidate.toLowerCase().contains("prologo")) return "Prólogo";
            if (candidate.toLowerCase().contains("extra")) return candidate.trim();
        }
        if (chapterId != null && chapterId.matches("[0-9]+(?:[._-][0-9]+)?")) return "Capítulo " + chapterId.replace('_', '.').replace('-', '.');
        return emptyTo(label, "Capítulo");
    }

    private static class PageData {
        final byte[] bytes;
        final String extension;
        PageData(byte[] bytes, String extension) { this.bytes = bytes; this.extension = extension; }
    }

    private PageData readPage(String src) throws Exception {
        if (src.startsWith("data:image/")) {
            int comma = src.indexOf(',');
            if (comma < 0) throw new Exception("Imagem inválida.");
            String head = src.substring(0, comma);
            String mime = head.substring(5).split(";")[0];
            String body = src.substring(comma + 1);
            byte[] bytes = head.contains(";base64") ? Base64.decode(body, Base64.DEFAULT) : Uri.decode(body).getBytes(StandardCharsets.ISO_8859_1);
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
        } finally { conn.disconnect(); }
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
        return m.length() > 120 ? m.substring(0, 120) : m;
    }

    private void copyDirectory(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) throw new Exception("Não foi possível concluir o download.");
            File[] children = src.listFiles();
            if (children != null) for (File child : children) copyDirectory(child, new File(dst, child.getName()));
            return;
        }
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
    }

    private static class OfflineItem {
        final File workDir;
        final File chapterDir;
        final JSONObject meta;
        OfflineItem(File workDir, File chapterDir, JSONObject meta) { this.workDir = workDir; this.chapterDir = chapterDir; this.meta = meta; }
    }

    private static class OfflineGroup {
        final String key;
        String title;
        String altName = "", synopsis = "", cover = "", type = "", status = "", author = "", artist = "", scan = "", language = "";
        long latest;
        final List<OfflineItem> chapters = new ArrayList<>();
        OfflineGroup(String key, String title, long latest) { this.key = key; this.title = title; this.latest = latest; }
    }

    private synchronized List<OfflineItem> listOfflineChapters() {
        List<OfflineItem> items = new ArrayList<>();
        File[] works = offlineRoot().listFiles(File::isDirectory);
        if (works == null) return items;
        for (File workDir : works) {
            recoverInterruptedDownloads(workDir);
            File[] chapters = workDir.listFiles(File::isDirectory);
            if (chapters == null) continue;
            for (File ch : chapters) {
                if (ch.getName().contains("_tmp_") || ch.getName().endsWith("_backup")) continue;
                File metaFile = new File(ch, "meta.json");
                if (!metaFile.exists()) continue;
                try { items.add(new OfflineItem(workDir, ch, new JSONObject(readText(metaFile)))); } catch (Exception ignored) {}
            }
        }
        Collections.sort(items, (a, b) -> Long.compare(b.meta.optLong("savedAt", 0), a.meta.optLong("savedAt", 0)));
        return items;
    }

    private void recoverInterruptedDownloads(File workDir) {
        File[] dirs = workDir.listFiles(File::isDirectory);
        if (dirs == null) return;
        for (File dir : dirs) {
            String name = dir.getName();
            if (name.contains("_tmp_")) {
                deleteRecursive(dir);
                continue;
            }
            if (!name.endsWith("_backup")) continue;
            String originalName = name.substring(0, name.length() - "_backup".length());
            File original = new File(workDir, originalName);
            if (original.exists()) deleteRecursive(dir);
            else dir.renameTo(original);
        }
    }

    private List<OfflineGroup> listOfflineGroups() {
        LinkedHashMap<String, OfflineGroup> map = new LinkedHashMap<>();
        for (OfflineItem item : listOfflineChapters()) {
            String key = item.workDir.getName();
            String title = item.meta.optString("workTitle", "").trim();
            if (title.isEmpty()) title = "Obra MP SCAN";
            long saved = item.meta.optLong("savedAt", 0);
            OfflineGroup group = map.get(key);
            if (group == null) {
                group = new OfflineGroup(key, title, saved);
                map.put(key, group);
            } else {
                if ((group.title.equals("Obra MP SCAN") || group.title.isEmpty()) && !title.equals("Obra MP SCAN")) group.title = title;
                if (saved > group.latest) group.latest = saved;
            }
            if (group.altName.isEmpty()) group.altName = item.meta.optString("workAltName", "");
            if (group.synopsis.isEmpty()) group.synopsis = item.meta.optString("workSynopsis", "");
            if (group.cover.isEmpty()) group.cover = item.meta.optString("workCover", "");
            if (group.type.isEmpty()) group.type = item.meta.optString("workType", "");
            if (group.status.isEmpty()) group.status = item.meta.optString("workStatus", "");
            if (group.author.isEmpty()) group.author = item.meta.optString("workAuthor", "");
            if (group.artist.isEmpty()) group.artist = item.meta.optString("workArtist", "");
            if (group.scan.isEmpty()) group.scan = item.meta.optString("workScan", "");
            if (group.language.isEmpty()) group.language = item.meta.optString("workLanguage", "");
            migrateMetaIfNeeded(item);
            group.chapters.add(item);
        }
        List<OfflineGroup> groups = new ArrayList<>(map.values());
        groups.sort((a, b) -> Long.compare(b.latest, a.latest));
        for (OfflineGroup g : groups) g.chapters.sort(chapterComparator());
        return groups;
    }

    private void migrateMetaIfNeeded(OfflineItem item) {
        try {
            JSONObject meta = item.meta;
            boolean changed = false;
            if (!meta.has("chapterLabel") || meta.optString("chapterLabel", "").trim().isEmpty()) {
                meta.put("chapterLabel", normalizeChapterLabel("", meta.optString("title", ""), meta.optString("chapterId", "")));
                changed = true;
            }
            if (changed) {
                try (OutputStream os = new FileOutputStream(new File(item.chapterDir, "meta.json"))) {
                    os.write(meta.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {}
    }

    private Comparator<OfflineItem> chapterComparator() {
        return (a, b) -> {
            Double na = chapterNumber(a.meta.optString("chapterLabel", ""));
            Double nb = chapterNumber(b.meta.optString("chapterLabel", ""));
            if (na != null && nb != null) return Double.compare(na, nb);
            if (na != null) return -1;
            if (nb != null) return 1;
            return a.meta.optString("chapterLabel", "").compareToIgnoreCase(b.meta.optString("chapterLabel", ""));
        };
    }

    private Double chapterNumber(String label) {
        Matcher m = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)").matcher(label == null ? "" : label);
        if (!m.find()) return null;
        try { return Double.parseDouble(m.group(1).replace(',', '.')); } catch (Exception e) { return null; }
    }

    private OfflineGroup findGroup(String workKey) {
        for (OfflineGroup group : listOfflineGroups()) if (group.key.equals(workKey)) return group;
        return null;
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
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String coverData(OfflineGroup group) {
        if (group == null || group.cover.isEmpty()) return "";
        for (OfflineItem item : group.chapters) {
            File file = new File(item.chapterDir, group.cover);
            if (!file.exists()) continue;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (InputStream in = new FileInputStream(file)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) bos.write(b, 0, n); }
                String ext = group.cover.toLowerCase();
                String mime = ext.endsWith(".png") ? "image/png" : ext.endsWith(".webp") ? "image/webp" : ext.endsWith(".gif") ? "image/gif" : "image/jpeg";
                return "data:" + mime + ";base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String commonCss() {
        return "*{box-sizing:border-box}html,body{margin:0;min-height:100%;background:#0b0810;color:#fff;font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}body{padding:18px 14px 44px;background:radial-gradient(circle at 80% -10%,rgba(133,64,174,.18),transparent 36%),radial-gradient(circle at -10% 30%,rgba(202,69,151,.11),transparent 32%),#0b0810}.shell{max-width:850px;margin:auto}.eyebrow{font-size:10px;font-weight:950;letter-spacing:.15em;color:#c890ee;text-transform:uppercase}.hero{padding:20px;border:1px solid #2f213a;border-radius:27px;background:linear-gradient(145deg,rgba(31,20,42,.96),rgba(17,12,24,.96));box-shadow:0 20px 55px rgba(0,0,0,.28)}h1{font-size:28px;line-height:1.08;margin:6px 0 8px}.muted{color:#ab9bb8;line-height:1.5;font-size:13px}.top-actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:15px}.btn{display:inline-flex;align-items:center;justify-content:center;gap:7px;text-decoration:none;border-radius:14px;padding:10px 13px;font-size:12px;font-weight:900}.btn-soft{background:#1e1527;border:1px solid #392849;color:#eee1f7}.btn-main{background:linear-gradient(135deg,#773fa9,#c94c9d);color:#fff}.stats{display:flex;gap:8px;margin-top:15px;overflow:auto}.stat{min-width:96px;padding:10px 12px;border-radius:15px;background:rgba(255,255,255,.035);border:1px solid #30223a}.stat b{display:block;font-size:18px}.stat span{font-size:10px;color:#a594b3}.section-title{display:flex;align-items:center;justify-content:space-between;gap:10px;margin:20px 2px 11px}.section-title h2{font-size:16px;margin:0}.section-title span{font-size:11px;color:#9e8cab}.badge{display:inline-flex;align-items:center;gap:5px;padding:6px 9px;border-radius:999px;background:rgba(127,71,167,.18);border:1px solid rgba(183,120,226,.3);color:#dfb9fb;font-size:10px;font-weight:900}.downloaded{color:#dcb8f7}.empty{text-align:center;margin:48px auto;max-width:560px;padding:28px 20px;border-radius:24px;background:#15101b;border:1px solid #2c2035}.empty .emoji{font-size:40px}.empty p{color:#a998b6;line-height:1.5}.footer-note{text-align:center;color:#766b7d;font-size:10px;margin-top:24px}@media(max-width:520px){body{padding:14px 11px 34px}.hero{padding:17px;border-radius:23px}h1{font-size:24px}.btn{padding:9px 11px}}";
    }

    private String appNavCss() {
        return "body{padding-bottom:104px}.app-head{display:flex;align-items:center;gap:12px;margin:2px 2px 16px}.app-logo{width:48px;height:48px;border-radius:16px;display:grid;place-items:center;background:linear-gradient(145deg,#713aa5,#d1539f);font-weight:1000;box-shadow:0 10px 28px rgba(154,66,174,.3)}.app-brand b{display:block;font-size:17px}.app-brand span{display:block;color:#9988a5;font-size:10px;margin-top:2px}.bottom-nav{position:fixed;z-index:30;left:50%;bottom:max(10px,env(safe-area-inset-bottom));transform:translateX(-50%);width:min(620px,calc(100% - 20px));display:grid;grid-template-columns:repeat(4,1fr);padding:7px;border:1px solid rgba(255,255,255,.12);border-radius:23px;background:rgba(18,12,25,.94);backdrop-filter:blur(22px);box-shadow:0 18px 48px rgba(0,0,0,.48)}.bottom-nav a{color:#887990;text-decoration:none;text-align:center;border-radius:17px;padding:8px 3px 7px;font-size:9px;font-weight:850}.bottom-nav a i{display:block;font-style:normal;font-size:19px;line-height:20px;margin-bottom:3px}.bottom-nav a.on{color:#fff;background:linear-gradient(145deg,rgba(117,61,161,.8),rgba(187,70,148,.72))}.option-list{display:grid;gap:10px;margin-top:16px}.option{display:flex;align-items:center;gap:12px;padding:15px;border:1px solid #30213b;border-radius:19px;background:#15101b;color:#fff;text-decoration:none}.option-icon{width:42px;height:42px;border-radius:14px;display:grid;place-items:center;background:#25182f;font-size:19px}.option-copy{flex:1;min-width:0}.option-copy b{display:block;font-size:13px}.option-copy span{display:block;color:#9f8fab;font-size:10px;margin-top:3px;line-height:1.4}.option em{color:#be91db;font-style:normal;font-size:20px}";
    }

    private String appHeader() {
        return "<header class='app-head'><div class='app-logo'>MP</div><div class='app-brand'><b>MP SCAN</b><span>Sua biblioteca de leitura</span></div></header>";
    }

    private String bottomNav(String active) {
        return "<nav class='bottom-nav'><a href='mpscan-offline://online'><i>⌂</i>Início</a><a class='" + ("library".equals(active) ? "on" : "") + "' href='mpscan-offline://library'><i>▦</i>Biblioteca</a><a href='mpscan-offline://online'><i>⌕</i>Busca</a><a class='" + ("more".equals(active) ? "on" : "") + "' href='mpscan-offline://more'><i>•••</i>Mais</a></nav>";
    }

    private void showHistory() {
        nativeScreen = "history";
        nativeWorkKey = "";
        List<OfflineItem> items = listOfflineChapters();
        items.sort((a,b) -> Long.compare(b.meta.optLong("lastReadAt", 0), a.meta.optLong("lastReadAt", 0)));
        StringBuilder rows = new StringBuilder();
        for (OfflineItem item : items) {
            long readAt = item.meta.optLong("lastReadAt", 0);
            if (readAt <= 0) continue;
            String work = Uri.encode(item.workDir.getName()), chapter = Uri.encode(item.chapterDir.getName());
            rows.append("<a class='option' href='mpscan-offline://read/").append(work).append("/").append(chapter).append("'><span class='option-icon'>▶</span><span class='option-copy'><b>").append(html(item.meta.optString("workTitle", "Obra MP SCAN"))).append("</b><span>").append(html(item.meta.optString("chapterLabel", "Capítulo"))).append(" • lido em ").append(html(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(readAt)))).append("</span></span><em>›</em></a>");
        }
        String content = rows.length() == 0 ? "<div class='empty'><div class='emoji'>◷</div><h2>Seu histórico está vazio</h2><p>Os capítulos offline que você abrir aparecerão aqui automaticamente.</p></div>" : "<div class='option-list'>" + rows + "</div>";
        String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'><style>" + commonCss() + appNavCss() + "</style></head><body><main class='shell'>" + appHeader() + "<section class='hero'><div class='eyebrow'>CONTINUE DE ONDE PAROU</div><h1>Histórico</h1><p class='muted'>Suas leituras recentes ficam organizadas aqui, inclusive sem internet.</p></section>" + content + "</main>" + bottomNav("history") + "</body></html>";
        web.loadDataWithBaseURL("https://app.mpscan.local/", page, "text/html", "UTF-8", null);
        hideLoading();
    }

    private void showMore() {
        nativeScreen = "more";
        nativeWorkKey = "";
        String permission = notificationsAllowed() ? "Permitidas neste aparelho" : "Toque para permitir";
        String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'><style>" + commonCss() + appNavCss() + "</style></head><body><main class='shell'>" + appHeader() + "<section class='hero'><div class='eyebrow'>AJUSTES DO APLICATIVO</div><h1>Mais</h1><p class='muted'>Controle permissões e acesse as funções principais do MP SCAN.</p><div class='option-list'><a class='option' href='mpscan-offline://notifications'><span class='option-icon'>🔔</span><span class='option-copy'><b>Notificações</b><span>" + permission + ". Receba avisos do aplicativo.</span></span><em>›</em></a><a class='option' href='mpscan-offline://online'><span class='option-icon'>⌂</span><span class='option-copy'><b>Abrir o site MP SCAN</b><span>Buscar obras, comentar e baixar novos capítulos.</span></span><em>›</em></a><a class='option' href='mpscan-offline://library'><span class='option-icon'>↓</span><span class='option-copy'><b>Downloads offline</b><span>Veja tudo que está guardado neste aparelho.</span></span><em>›</em></a></div></section></main>" + bottomNav("more") + "</body></html>";
        web.loadDataWithBaseURL("https://app.mpscan.local/", page, "text/html", "UTF-8", null);
        hideLoading();
    }

    private void showOfflineLibrary() {
        nativeScreen = "library";
        nativeWorkKey = "";
        List<OfflineGroup> groups = listOfflineGroups();
        int totalChapters = 0;
        int totalPages = 0;
        StringBuilder cards = new StringBuilder();

        for (OfflineGroup group : groups) {
            totalChapters += group.chapters.size();
            int pages = 0;
            StringBuilder previews = new StringBuilder();
            int shown = 0;
            for (OfflineItem item : group.chapters) {
                pages += item.meta.optInt("pageCount", 0);
                if (shown < 4) {
                    if (shown > 0) previews.append(" • ");
                    previews.append(html(item.meta.optString("chapterLabel", "Capítulo")));
                    shown++;
                }
            }
            totalPages += pages;
            String workKey = Uri.encode(group.key);
            String latestDate = group.latest > 0 ? DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(group.latest)) : "";

            String cover = coverData(group);
            cards.append("<a class='work-card' href='mpscan-offline://work/").append(workKey).append("'>")
                .append("<div class='work-mark'>").append(cover.isEmpty() ? "<span>MP</span>" : "<img src='" + cover + "' alt=''>").append("</div><div class='work-copy'><div class='row'><strong>").append(html(group.title)).append("</strong><span class='arrow'>›</span></div>")
                .append(group.altName.isEmpty() ? "" : "<div class='alt'>" + html(group.altName) + "</div>")
                .append("<div class='chips'><span>✓ ").append(group.chapters.size()).append(group.chapters.size() == 1 ? " capítulo baixado" : " capítulos baixados").append("</span><span>").append(pages).append(" páginas</span></div>")
                .append("<p>").append(previews.length() == 0 ? "Capítulos salvos" : previews).append(group.chapters.size() > 4 ? " • …" : "").append("</p>")
                .append(latestDate.isEmpty() ? "" : "<small>Último download: " + html(latestDate) + "</small>")
                .append("</div></a>");
        }

        String content = groups.isEmpty()
            ? "<div class='empty'><div class='emoji'>📥</div><h2>Nenhum capítulo baixado</h2><p>Abra uma obra no aplicativo e toque em <b>Baixar capítulo</b>. Aqui aparecerá uma pasta separada para cada obra.</p></div>"
            : "<div class='work-grid'>" + cards + "</div>";

        String css = commonCss() + appNavCss() + ".work-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.work-card{text-decoration:none;color:#fff;padding:13px;border-radius:22px;background:linear-gradient(145deg,#191121,#120d18);border:1px solid #30213b;display:grid;grid-template-columns:72px minmax(0,1fr);gap:12px;box-shadow:0 14px 35px rgba(0,0,0,.18)}.work-mark{width:72px;aspect-ratio:2/3;border-radius:15px;background:linear-gradient(145deg,#6d379d,#b64b91);display:grid;place-items:center;overflow:hidden;font-weight:950;box-shadow:inset 0 0 0 1px rgba(255,255,255,.12)}.work-mark img{width:100%;height:100%;object-fit:cover}.work-copy{min-width:0}.row{display:flex;align-items:flex-start;gap:8px}.row strong{font-size:14px;line-height:1.3;flex:1}.alt{font-size:10px;color:#9f8bad;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:3px}.arrow{font-size:24px;line-height:17px;color:#be91db}.chips{display:flex;gap:6px;flex-wrap:wrap;margin-top:7px}.chips span{font-size:9px;font-weight:850;color:#cdb8db;background:#21162b;border:1px solid #342443;border-radius:999px;padding:5px 7px}.work-copy p{font-size:10px;color:#a996b6;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin:8px 0 5px}.work-copy small{font-size:9px;color:#756b7a}@media(max-width:620px){.work-grid{grid-template-columns:1fr}}";

        String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'><style>" + css + "</style></head><body><main class='shell'>" + appHeader()
            + "<section class='hero'><div class='eyebrow'>BIBLIOTECA OFFLINE</div><h1>Minha biblioteca</h1><p class='muted'>Suas obras e capítulos baixados ficam disponíveis mesmo quando você sair do aplicativo ou estiver sem internet.</p>"
            + "<div class='stats'><div class='stat'><b>" + groups.size() + "</b><span>obras</span></div><div class='stat'><b>" + totalChapters + "</b><span>capítulos</span></div><div class='stat'><b>" + totalPages + "</b><span>páginas salvas</span></div></div>"
            + "<div class='top-actions'><a class='btn btn-soft' href='mpscan-offline://online'>← Voltar para o MP SCAN</a></div></section>"
            + "<div class='section-title'><h2>Obras com downloads</h2><span>Somente capítulos salvos</span></div>" + content
            + "<div class='footer-note'>Os downloads ficam armazenados no aplicativo.</div></main>" + bottomNav("library") + "</body></html>";
        web.loadDataWithBaseURL("https://app.mpscan.local/", page, "text/html", "UTF-8", null);
        hideLoading();
    }

    private void showOfflineWork(String workKey) {
        OfflineGroup group = findGroup(workKey);
        if (group == null) { showOfflineLibrary(); return; }
        nativeScreen = "work";
        nativeWorkKey = workKey;

        int totalPages = 0;
        StringBuilder list = new StringBuilder();
        for (OfflineItem item : group.chapters) {
            JSONObject meta = item.meta;
            int pages = meta.optInt("pageCount", 0);
            totalPages += pages;
            String label = meta.optString("chapterLabel", "Capítulo");
            String title = meta.optString("title", label);
            if (title.equalsIgnoreCase(label)) title = "";
            long savedAt = meta.optLong("savedAt", 0);
            String date = savedAt > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(savedAt)) : "";
            String encodedWork = Uri.encode(item.workDir.getName());
            String encodedChapter = Uri.encode(item.chapterDir.getName());

            list.append("<article class='chapter-card'><div class='num'>").append(html(label)).append("</div><div class='chapter-info'>")
                .append("<div class='chapter-top'><div><strong>").append(html(label)).append("</strong>")
                .append(title.isEmpty() ? "" : "<p>" + html(title) + "</p>").append("</div><span class='saved'>✓ Baixado</span></div>")
                .append("<div class='meta'><span>📄 ").append(pages).append(" páginas</span>")
                .append(date.isEmpty() ? "" : "<span>🕒 " + html(date) + "</span>").append("</div>")
                .append("<div class='chapter-actions'><a class='btn btn-main' href='mpscan-offline://read/").append(encodedWork).append("/").append(encodedChapter).append("'>▶ Ler offline</a>")
                .append("<a class='btn btn-soft' href='mpscan-offline://delete/").append(encodedWork).append("/").append(encodedChapter).append("'>Excluir</a></div></div></article>");
        }

        String cover = coverData(group);
        String facts = "";
        for (String value : new String[]{group.type, group.status, group.author.isEmpty() ? "" : "Autor: " + group.author, group.scan.isEmpty() ? "" : "Scan: " + group.scan}) if (!value.isEmpty()) facts += "<span>" + html(value) + "</span>";
        String css = commonCss() + ".work-identity{display:grid;grid-template-columns:105px minmax(0,1fr);gap:16px;align-items:start}.poster{width:105px;aspect-ratio:2/3;border-radius:18px;overflow:hidden;background:linear-gradient(145deg,#713aa2,#c14998);display:grid;place-items:center;font-weight:950}.poster img{width:100%;height:100%;object-fit:cover}.identity-copy h1{margin-top:4px}.alt-name{color:#bca9c8;font-size:12px;margin-top:-2px}.facts{display:flex;gap:6px;flex-wrap:wrap;margin-top:10px}.facts span{font-size:9px;font-weight:850;padding:6px 8px;border:1px solid #3a2947;background:#1d1425;border-radius:999px}.synopsis{margin-top:13px;padding-top:12px;border-top:1px solid #34253f;color:#b4a4bf;font-size:12px;line-height:1.55}.work-summary{display:flex;align-items:center;gap:14px;margin-top:15px;padding:13px;border-radius:18px;background:rgba(255,255,255,.03);border:1px solid #30213b}.folder{width:54px;height:54px;display:grid;place-items:center;border-radius:17px;background:linear-gradient(145deg,#713aa2,#c14998);font-size:24px}.work-summary strong{font-size:14px}.work-summary p{font-size:11px;color:#a695b3;margin:4px 0 0}.chapter-list{display:grid;gap:11px}.chapter-card{display:grid;grid-template-columns:82px minmax(0,1fr);gap:13px;padding:14px;border-radius:22px;background:linear-gradient(145deg,#191121,#110c17);border:1px solid #30213b}.num{height:76px;border-radius:17px;display:grid;place-items:center;text-align:center;padding:8px;background:linear-gradient(145deg,#2d1c3b,#201329);border:1px solid #412b52;color:#e2c0f8;font-size:12px;font-weight:950}.chapter-info{min-width:0}.chapter-top{display:flex;justify-content:space-between;gap:10px}.chapter-top strong{font-size:15px}.chapter-top p{margin:4px 0 0;color:#a896b4;font-size:11px;line-height:1.35}.saved{white-space:nowrap;align-self:flex-start;padding:5px 8px;border-radius:999px;background:rgba(91,47,119,.24);border:1px solid rgba(181,120,220,.35);color:#dcb8f7;font-size:9px;font-weight:950}.meta{display:flex;gap:7px;flex-wrap:wrap;margin-top:9px}.meta span{font-size:9px;color:#9f8eac;background:#1b1322;border-radius:999px;padding:5px 7px}.chapter-actions{display:flex;gap:7px;margin-top:10px}.chapter-actions .btn{padding:8px 10px;font-size:10px}@media(max-width:520px){.work-identity{grid-template-columns:82px minmax(0,1fr)}.poster{width:82px}.chapter-card{grid-template-columns:64px minmax(0,1fr);gap:10px;padding:11px}.num{height:68px;font-size:10px}.chapter-top{flex-direction:column}.saved{align-self:flex-start}.chapter-actions{flex-wrap:wrap}}";

        String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'><style>" + css + "</style></head><body><main class='shell'>"
            + "<section class='hero'><div class='work-identity'><div class='poster'>" + (cover.isEmpty() ? "MP" : "<img src='" + cover + "' alt=''>") + "</div><div class='identity-copy'><div class='eyebrow'>OBRA DISPONÍVEL OFFLINE</div><h1>" + html(group.title) + "</h1>" + (group.altName.isEmpty() ? "" : "<div class='alt-name'>" + html(group.altName) + "</div>") + "<div class='facts'>" + facts + "</div></div></div>" + (group.synopsis.isEmpty() ? "" : "<div class='synopsis'>" + html(group.synopsis) + "</div>")
            + "<div class='work-summary'><div class='folder'>📚</div><div><strong>" + group.chapters.size() + (group.chapters.size() == 1 ? " capítulo salvo" : " capítulos salvos") + "</strong><p>" + totalPages + " páginas disponíveis offline</p></div></div>"
            + "<div class='top-actions'><a class='btn btn-soft' href='mpscan-offline://library'>← Todas as obras</a><a class='btn btn-main' href='mpscan-offline://online'>Abrir MP SCAN</a></div></section>"
            + "<div class='section-title'><h2>Capítulos disponíveis</h2><span class='badge'>✓ Baixados</span></div><section class='chapter-list'>" + list + "</section>"
            + "<div class='footer-note'>Você pode excluir um capítulo sem apagar os outros desta obra.</div></main></body></html>";
        web.loadDataWithBaseURL("https://app.mpscan.local/", page, "text/html", "UTF-8", null);
        hideLoading();
    }

    private void showOfflineReader(String workKey, String chapterKey) {
        File ch = new File(new File(offlineRoot(), workKey), chapterKey);
        File metaFile = new File(ch, "meta.json");
        if (!metaFile.exists()) { showOfflineLibrary(); return; }
        try {
            JSONObject meta = new JSONObject(readText(metaFile));
            JSONArray files = meta.optJSONArray("files");
            if (files == null || files.length() == 0) { showOfflineLibrary(); return; }
            meta.put("lastReadAt", System.currentTimeMillis());
            try (OutputStream historyOut = new FileOutputStream(metaFile)) {
                historyOut.write(meta.toString().getBytes(StandardCharsets.UTF_8));
            }
            nativeScreen = "reader";
            nativeWorkKey = workKey;
            StringBuilder imgs = new StringBuilder();
            for (int i = 0; i < files.length(); i++) {
                String name = files.optString(i, "");
                if (!name.isEmpty()) imgs.append("<img loading='eager' src='").append(html(name)).append("' alt='Página ").append(i + 1).append("'>");
            }
            String label = meta.optString("chapterLabel", normalizeChapterLabel("", meta.optString("title", ""), meta.optString("chapterId", "")));
            String workTitle = meta.optString("workTitle", "Obra MP SCAN");
            String title = meta.optString("title", label);
            OfflineGroup group = findGroup(workKey);
            OfflineItem previous = null, next = null;
            if (group != null) {
                for (int i = 0; i < group.chapters.size(); i++) {
                    if (group.chapters.get(i).chapterDir.getName().equals(chapterKey)) {
                        if (i > 0) previous = group.chapters.get(i - 1);
                        if (i + 1 < group.chapters.size()) next = group.chapters.get(i + 1);
                        break;
                    }
                }
            }
            String workHref = "mpscan-offline://work/" + Uri.encode(workKey);
            String previousButton = previous == null ? "<span></span>" : "<a href='mpscan-offline://read/" + Uri.encode(workKey) + "/" + Uri.encode(previous.chapterDir.getName()) + "'>← " + html(previous.meta.optString("chapterLabel", "Anterior")) + "</a>";
            String nextHref = next == null ? "" : "mpscan-offline://read/" + Uri.encode(workKey) + "/" + Uri.encode(next.chapterDir.getName());
            String nextButton = next == null ? "<span></span>" : "<a class='primary' href='" + nextHref + "'>" + html(next.meta.optString("chapterLabel", "Próximo")) + " →</a>";
            String navigation = "<nav>" + previousButton + "<a href='" + workHref + "'>☰ Obra</a>" + nextButton + "</nav>";
            String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover,user-scalable=yes'><style>*{box-sizing:border-box}html,body{margin:0;background:#08060b;color:#fff;font-family:system-ui,-apple-system,sans-serif}.bar{position:sticky;top:0;z-index:5;display:grid;grid-template-columns:auto minmax(0,1fr);gap:10px;align-items:center;padding:11px 12px;background:rgba(12,8,16,.95);backdrop-filter:blur(16px);border-bottom:1px solid #2c2035}.bar a,nav a{color:#fff;text-decoration:none;background:#1d1425;border:1px solid #392747;border-radius:12px;padding:9px 10px;font-weight:900;font-size:11px}.copy{min-width:0}.copy b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:13px}.copy span{display:block;font-size:10px;color:#aa98b7;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:2px}nav{display:grid;grid-template-columns:1fr auto 1fr;gap:8px;padding:11px 12px;background:#0d0912}nav>*:last-child{justify-self:end}nav .primary{background:linear-gradient(135deg,#713da5,#bd4994)}.pages{width:100%;margin:0 auto}.pages img{display:block;width:100%;max-width:100%;height:auto;margin:0 auto;background:#130d18}</style></head><body><header class='bar'><a href='" + workHref + "'>← Obra</a><div class='copy'><b>" + html(label) + (title.equalsIgnoreCase(label) ? "" : " • " + html(title)) + "</b><span>" + html(workTitle) + " • ✓ baixado • " + files.length() + " páginas</span></div></header>" + navigation + "<main class='pages'>" + imgs + "</main></body></html>";
            String base = Uri.fromFile(ch).toString();
            if (!base.endsWith("/")) base += "/";
            web.loadDataWithBaseURL(base, page, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir este capítulo.", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Capítulo removido dos downloads", Toast.LENGTH_SHORT).show();
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
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
            if (!nativeWorkKey.isEmpty()) showOfflineWork(nativeWorkKey);
            else showOfflineLibrary();
            return;
        }
        if ("work".equals(nativeScreen)) { showOfflineLibrary(); return; }
        if ("history".equals(nativeScreen) || "more".equals(nativeScreen)) { showNativeApp(); return; }
        if ("library".equals(nativeScreen)) {
            nativeScreen = "";
            nativeWorkKey = "";
            showNativeApp();
            return;
        }
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
