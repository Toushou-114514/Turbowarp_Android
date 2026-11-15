package app.turbowarp.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditorActivity extends Activity {

    public static final String EXTRA_PROJECT_ID = "project_id";
    private static final String ORIGIN = "https://tw.local";

    private WebView web;
    private String projectId;
    private FileOutputStream saveStream;
    private File saveTmp;
    private long saveBytes;
    private boolean diagnosticShown;
    private String pendingTitle;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private ValueCallback<Uri[]> fileChooserCallback;
    private final List<String> consoleErrors = new ArrayList<>();
    private final LinkedHashSet<String> missingAssets = new LinkedHashSet<>();

    private static final Map<String, String> MIME = new HashMap<>();
    static {
        MIME.put("html", "text/html");
        MIME.put("js", "application/javascript");
        MIME.put("css", "text/css");
        MIME.put("json", "application/json");
        MIME.put("webmanifest", "application/manifest+json");
        MIME.put("svg", "image/svg+xml");
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("webp", "image/webp");
        MIME.put("ico", "image/x-icon");
        MIME.put("mp3", "audio/mpeg");
        MIME.put("wav", "audio/wav");
        MIME.put("ogg", "audio/ogg");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
        MIME.put("ttf", "font/ttf");
        MIME.put("otf", "font/otf");
        MIME.put("wasm", "application/wasm");
        MIME.put("map", "application/json");
        MIME.put("txt", "text/plain");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyTheme(this);
        Window w = getWindow();
        w.setStatusBarColor(Ui.ACCENT_DARK);
        w.setNavigationBarColor(0xFF000000);

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        s.setUserAgentString(s.getUserAgentString() + " TurbowarpAndroid/1.0");

        web.setBackgroundColor(Color.WHITE);
        web.addJavascriptInterface(new Bridge(), "TWAndroid");
        web.setWebViewClient(new Client());
        web.setWebChromeClient(new Chrome());
        if (Build.VERSION.SDK_INT >= 19) WebView.setWebContentsDebuggingEnabled(true);

        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        web.loadUrl(ORIGIN + "/index.html" + (projectId != null ? "?twload=1" : ""));
    }

    private class Client extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (u == null || u.getHost() == null || !u.getHost().equals("tw.local")) return null;
            String path = u.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) path = "/index.html";
            if (path.equals("/__local/project.sb3")) return serveLocalProject();
            if (path.equals("/addons")) path = "/addons.html";
            return serveAsset(path);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (u != null && "tw.local".equals(u.getHost())) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (ActivityNotFoundException e) {
                Logx.e("openlink", e);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (projectId != null) {
                view.evaluateJavascript("window.__twLoad && window.__twLoad();", null);
            }
        }
    }

    private WebResourceResponse serveLocalProject() {
        if (projectId == null) return notFound();
        Store.Project p = Store.get(this, projectId);
        if (p == null) return notFound();
        File f = p.projectFile(this);
        if (!f.exists()) return notFound();
        try {
            WebResourceResponse r = new WebResourceResponse("application/octet-stream", null,
                    new FileInputStream(f));
            if (Build.VERSION.SDK_INT >= 21) {
                Map<String, String> h = new HashMap<>();
                h.put("Cache-Control", "no-store");
                r.setResponseHeaders(h);
            }
            return r;
        } catch (Exception e) {
            Logx.e("serveLocal", e);
            return notFound();
        }
    }

    private WebResourceResponse serveAsset(String path) {
        String rel = path.startsWith("/") ? path.substring(1) : path;
        try {
            InputStream in = getAssets().open("www/" + rel);
            String mime = "application/octet-stream";
            int dot = rel.lastIndexOf('.');
            if (dot >= 0) {
                String ext = rel.substring(dot + 1).toLowerCase(Locale.US);
                String m = MIME.get(ext);
                if (m != null) mime = m;
            }
            boolean text = mime.startsWith("text/") || mime.contains("javascript")
                    || mime.contains("json") || mime.contains("xml");
            WebResourceResponse r = new WebResourceResponse(mime, text ? "utf-8" : null, in);
            if (Build.VERSION.SDK_INT >= 21) {
                Map<String, String> h = new HashMap<>();
                h.put("Cache-Control", "no-cache");
                r.setResponseHeaders(h);
            }
            return r;
        } catch (IOException e) {
            synchronized (missingAssets) {
                if (missingAssets.size() < 200) missingAssets.add(rel);
            }
            return notFound();
        }
    }

    private WebResourceResponse notFound() {
        if (Build.VERSION.SDK_INT >= 21) {
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                    new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
        }
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream(new byte[0]));
    }

    private class Chrome extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage m) {
            String line = m.messageLevel() + ": " + m.message()
                    + " @" + m.sourceId() + ":" + m.lineNumber();
            Logx.d("console " + line);
            if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                synchronized (consoleErrors) {
                    if (consoleErrors.size() < 40) consoleErrors.add(line);
                }
            }
            return true;
        }

        @Override
        public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                         FileChooserParams params) {
            if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = cb;
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, 3001);
                return true;
            } catch (Exception e) {
                Logx.e("chooser", e);
                fileChooserCallback = null;
                return false;
            }
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 3001) {
            Uri[] out = null;
            if (res == RESULT_OK && data != null && data.getData() != null) {
                out = new Uri[]{data.getData()};
            }
            if (fileChooserCallback != null) {
                fileChooserCallback.onReceiveValue(out);
                fileChooserCallback = null;
            }
        }
    }

    private void toast(final String s) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(EditorActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String nextTitle() {
        return getString(R.string.untitled_prefix) + " " + (Store.list(this).size() + 1);
    }

    private void moveFile(File from, File to) {
        to.getParentFile().mkdirs();
        if (to.exists()) to.delete();
        if (!from.renameTo(to)) {
            byte[] d = Store.readAll(from);
            if (d != null) Store.writeAll(to, d);
            from.delete();
        }
    }

    private void showDiagnostics(final String msg) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                if (diagnosticShown || isFinishing()) return;
                diagnosticShown = true;
                StringBuilder sb = new StringBuilder(msg).append("\n\n");
                synchronized (missingAssets) {
                    if (!missingAssets.isEmpty()) {
                        sb.append("Missing assets (").append(missingAssets.size()).append("):\n");
                        int i = 0;
                        for (String m : missingAssets) {
                            if (i++ >= 8) { sb.append("  ...\n"); break; }
                            sb.append("  ").append(m).append('\n');
                        }
                        sb.append('\n');
                    }
                }
                synchronized (consoleErrors) {
                    if (!consoleErrors.isEmpty()) {
                        sb.append("Console errors:\n");
                        int i = 0;
                        for (String e : consoleErrors) {
                            if (i++ >= 6) { sb.append("  ...\n"); break; }
                            sb.append("  ").append(e).append('\n');
                        }
                    }
                }
                TextView tv = Ui.text(EditorActivity.this, sb.toString(), 12, 0xFF222222, false);
                tv.setTextIsSelectable(true);
                int pad = Ui.dp(EditorActivity.this, 18);
                tv.setPadding(pad, pad, pad, pad);
                android.widget.ScrollView sv = new android.widget.ScrollView(EditorActivity.this);
                sv.addView(tv);
                new android.app.AlertDialog.Builder(EditorActivity.this)
                        .setTitle("编辑器问题 / Editor problem")
                        .setView(sv)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            String u = web.getUrl();
            if (u != null && !u.endsWith("/index.html") && !u.contains("twload=1")) {
                web.goBack();
                return;
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        try {
            if (saveStream != null) saveStream.close();
        } catch (Exception ignored) {
        }
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
        }
        super.onDestroy();
    }

    /** JS -> native bridge. Called on the JavaBridge thread. */
    private class Bridge {
        @JavascriptInterface
        public void log(String msg) {
            Logx.d("js: " + msg);
        }

        @JavascriptInterface
        public void toast(String msg) {
            EditorActivity.this.toast(msg);
        }

        @JavascriptInterface
        public void reportProblem(String msg) {
            showDiagnostics(msg);
        }

        @JavascriptInterface
        public String label(String key) {
            if ("save".equals(key)) return getString(R.string.action_save);
            if ("saving".equals(key)) return getString(R.string.saving);
            if ("failed".equals(key)) return getString(R.string.save_failed);
            if ("notready".equals(key)) return getString(R.string.not_ready);
            if ("loadfailed".equals(key)) return getString(R.string.load_failed);
            return "";
        }

        @JavascriptInterface
        public String locale() {
            return Prefs.localeTag(EditorActivity.this);
        }

        @JavascriptInterface
        public String accent() {
            return String.format("#%06X", 0xFFFFFF & Prefs.accent(EditorActivity.this));
        }

        @JavascriptInterface
        public String dark() {
            return Ui.isDark(EditorActivity.this) ? "1" : "0";
        }

        @JavascriptInterface
        public String highContrast() {
            return Prefs.highContrast(EditorActivity.this) ? "1" : "0";
        }

        @JavascriptInterface
        public String projectTitle() {
            if (projectId == null) return nextTitle();
            Store.Project p = Store.get(EditorActivity.this, projectId);
            return p == null ? nextTitle() : p.title;
        }

        @JavascriptInterface
        public void setTitle(String title) {
            if (title == null) return;
            String t = title.trim();
            if (t.isEmpty()) return;
            if (projectId == null) {
                pendingTitle = t;
            } else if (!t.equals(projectTitle())) {
                Store.rename(EditorActivity.this, projectId, t);
            }
        }

        @JavascriptInterface
        public synchronized void startSave(String title) {
            try {
                if (saveStream != null) {
                    try { saveStream.close(); } catch (Exception ignored) { }
                }
                if (title != null && !title.trim().isEmpty()) pendingTitle = title.trim();
                saveTmp = new File(getCacheDir(), "saving.sb3");
                saveStream = new FileOutputStream(saveTmp);
                saveBytes = 0;
            } catch (Exception e) {
                Logx.e("startSave", e);
                saveStream = null;
            }
        }

        @JavascriptInterface
        public synchronized void chunk(String b64) {
            if (saveStream == null) return;
            try {
                byte[] d = Base64.decode(b64, Base64.DEFAULT);
                saveStream.write(d);
                saveBytes += d.length;
            } catch (Exception e) {
                Logx.e("chunk", e);
            }
        }

        @JavascriptInterface
        public synchronized void endSave(String thumbDataUrl) {
            try {
                if (saveStream == null) {
                    toast(getString(R.string.save_failed));
                    return;
                }
                saveStream.close();
                saveStream = null;
                if (saveBytes == 0) {
                    toast(getString(R.string.save_failed));
                    return;
                }
                String wanted = pendingTitle != null && !pendingTitle.trim().isEmpty()
                        ? pendingTitle.trim() : nextTitle();
                if (projectId == null) {
                    String id = Store.newId();
                    Store.Project p = Store.upsert(EditorActivity.this, id, wanted);
                    moveFile(saveTmp, p.projectFile(EditorActivity.this));
                    projectId = id;
                } else {
                    Store.Project p = Store.get(EditorActivity.this, projectId);
                    if (p == null) p = Store.upsert(EditorActivity.this, projectId, wanted);
                    moveFile(saveTmp, p.projectFile(EditorActivity.this));
                    Store.upsert(EditorActivity.this, projectId, p.title);
                }
                Store.setLastProject(EditorActivity.this, projectId);
                if (thumbDataUrl != null && thumbDataUrl.startsWith("data:image")) {
                    int comma = thumbDataUrl.indexOf(',');
                    if (comma > 0) {
                        byte[] png = Base64.decode(thumbDataUrl.substring(comma + 1), Base64.DEFAULT);
                        Store.Project p2 = Store.get(EditorActivity.this, projectId);
                        if (p2 != null) Store.writeAll(p2.thumbFile(EditorActivity.this), png);
                    }
                }
                toast(getString(R.string.saved));
            } catch (Exception e) {
                Logx.e("endSave", e);
                toast(getString(R.string.save_failed));
            }
        }
    }
}
