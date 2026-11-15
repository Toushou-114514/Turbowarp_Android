package app.turbowarp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private GridView grid;
    private TextView emptyView;
    private final List<Store.Project> items = new ArrayList<>();
    private Adapter adapter;
    private static final int REQ_IMPORT = 1001;
    private String pendingExportId;
    private String uiStamp;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyTheme(this);
        if (Ui.DARK) setTheme(R.style.AppTheme_Dark);
        uiStamp = stamp();

        Window w = getWindow();
        w.setStatusBarColor(Ui.DARK ? 0xFF000000 : Ui.ACCENT_DARK);
        w.setNavigationBarColor(0xFF000000);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        col.addView(buildTopBar());

        grid = new GridView(this);
        int cols = computeColumns();
        grid.setNumColumns(cols);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(Ui.dp(this, 14));
        grid.setVerticalSpacing(Ui.dp(this, 14));
        int pad = Ui.dp(this, 18);
        grid.setPadding(pad, pad, pad, Ui.dp(this, 90));
        grid.setClipToPadding(false);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openProject(items.get(position).id);
            }
        });
        col.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        emptyView = Ui.text(this, getString(R.string.empty_hint), 15, Ui.TEXT_LIGHT, false);
        emptyView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.gravity = Gravity.CENTER;
        ep.bottomMargin = Ui.dp(this, 80);
        root.addView(emptyView, ep);

        root.addView(buildFab(), fabParams());

        setContentView(root);

        adapter = new Adapter();
        grid.setAdapter(adapter);
    }

    private String stamp() {
        return Prefs.accent(this) + "|" + Prefs.highContrast(this) + "|" + Prefs.language(this)
                + "|" + Prefs.theme(this);
    }

    private int computeColumns() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        int cols = Math.max(2, widthDp / 250);
        return Math.min(cols, 6);
    }

    private View buildTopBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Ui.ACCENT);
        bar.setElevation(Ui.dp(this, 4));
        int h = Ui.dp(this, 60);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h));

        TextView title = Ui.text(this, getString(R.string.app_name), 19, Color.WHITE, true);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.CENTER;
        bar.addView(title, tp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ap.gravity = Gravity.END;
        ap.rightMargin = Ui.dp(this, 6);
        bar.addView(actions, ap);

        actions.addView(iconAction(R.drawable.ic_import, R.string.action_import, new View.OnClickListener() {
            @Override
            public void onClick(View v) { showImportDialog(); }
        }));
        actions.addView(iconAction(R.drawable.ic_settings, R.string.settings_title, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        }));

        return bar;
    }

    private View iconAction(int drawable, int descRes, View.OnClickListener l) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(drawable);
        iv.setColorFilter(Color.WHITE);
        int p = Ui.dp(this, 10);
        iv.setPadding(p, p, p, p);
        iv.setContentDescription(getString(descRes));
        iv.setOnClickListener(l);
        iv.setBackground(Ui.round(0x22FFFFFF, 10, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Ui.dp(this, 40), Ui.dp(this, 40));
        lp.leftMargin = Ui.dp(this, 6);
        iv.setLayoutParams(lp);
        return iv;
    }

    private View buildFab() {
        TextView fab = Ui.text(this, "+", 34, Color.WHITE, false);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.round(Ui.ACCENT, 30, this));
        fab.setElevation(Ui.dp(this, 8));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
            }
        });
        return fab;
    }

    private FrameLayout.LayoutParams fabParams() {
        int s = Ui.dp(this, 60);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(s, s);
        p.gravity = Gravity.BOTTOM | Gravity.END;
        p.rightMargin = Ui.dp(this, 24);
        p.bottomMargin = Ui.dp(this, 24);
        return p;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uiStamp != null && !uiStamp.equals(stamp())) {
            recreate();
            return;
        }
        reload();
    }

    private void reload() {
        List<Store.Project> fresh = Store.list(this);
        items.clear();
        items.addAll(fresh);
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        grid.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openProject(String id) {
        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra(EditorActivity.EXTRA_PROJECT_ID, id);
        startActivity(i);
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Store.Project p = items.get(position);
            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Ui.roundStroke(Ui.CARD, 14, Ui.DIVIDER, MainActivity.this));
            card.setElevation(Ui.dp(MainActivity.this, 3));
            int cpad = Ui.dp(MainActivity.this, 12);
            card.setPadding(cpad, cpad, cpad, Ui.dp(MainActivity.this, 14));

            FrameLayout thumbWrap = new FrameLayout(MainActivity.this);
            int th = Ui.dp(MainActivity.this, 132);
            LinearLayout.LayoutParams twp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, th);
            card.addView(thumbWrap, twp);

            ImageView img = new ImageView(MainActivity.this);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setBackground(Ui.round(Ui.THUMB_BG, 10, MainActivity.this));
            File tf = p.thumbFile(MainActivity.this);
            if (tf.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(tf.getAbsolutePath());
                if (bm != null) img.setImageBitmap(bm);
            } else {
                img.setImageResource(R.mipmap.ic_launcher);
                int ip = Ui.dp(MainActivity.this, 34);
                img.setPadding(ip, ip, ip, ip);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
            thumbWrap.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView dots = Ui.text(MainActivity.this, "\u22EE", 20,
                    Ui.HIGH_CONTRAST ? Ui.TEXT : (Ui.DARK ? 0xFF7E8894 : 0xFFB9C0CC), false);
            dots.setGravity(Gravity.CENTER);
            dots.setPadding(Ui.dp(MainActivity.this, 6), 0, Ui.dp(MainActivity.this, 6), 0);
            FrameLayout.LayoutParams dp2 = new FrameLayout.LayoutParams(
                    Ui.dp(MainActivity.this, 34), Ui.dp(MainActivity.this, 34));
            dp2.gravity = Gravity.TOP | Gravity.END;
            dots.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showMenu(v, p); }
            });
            thumbWrap.addView(dots, dp2);

            TextView title = Ui.text(MainActivity.this, p.title, 16, Ui.TEXT, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Ui.dp(MainActivity.this, 10);
            card.addView(title, tlp);

            SimpleDateFormat f = new SimpleDateFormat("yyyy/M/d", Locale.US);
            TextView date = Ui.text(MainActivity.this,
                    getString(R.string.modified_on, f.format(new Date(p.modified))),
                    12, Ui.TEXT_LIGHT, false);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = Ui.dp(MainActivity.this, 4);
            card.addView(date, dlp);

            return card;
        }
    }

    private void showMenu(View anchor, final Store.Project p) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, R.string.action_export);
        pm.getMenu().add(0, 2, 1, R.string.action_rename);
        pm.getMenu().add(0, 3, 2, R.string.action_share);
        pm.getMenu().add(0, 4, 3, R.string.action_delete);
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                switch (item.getItemId()) {
                    case 1: exportProject(p); return true;
                    case 2: renameProject(p); return true;
                    case 3: shareProject(p); return true;
                    case 4: confirmDelete(p); return true;
                }
                return false;
            }
        });
        pm.show();
    }

    private void renameProject(final Store.Project p) {
        final EditText ed = new EditText(this);
        ed.setInputType(InputType.TYPE_CLASS_TEXT);
        ed.setText(p.title);
        ed.setSelection(ed.getText().length());
        int pad = Ui.dp(this, 20);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(pad, Ui.dp(this, 8), pad, 0);
        wrap.addView(ed);
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_rename)
                .setView(wrap)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String t = ed.getText().toString().trim();
                        if (t.length() > 0) {
                            Store.rename(MainActivity.this, p.id, t);
                            reload();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(final Store.Project p) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_msg, p.title))
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Store.delete(MainActivity.this, p.id);
                        reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String safeName(String s) {
        if (s == null) s = "project";
        s = s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (s.length() == 0) s = "project";
        return s;
    }

    private void exportProject(Store.Project p) {
        byte[] data = Store.readAll(p.projectFile(this));
        if (data == null) {
            toast(getString(R.string.export_failed));
            return;
        }
        String name = safeName(p.title) + ".sb3";
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("insert failed");
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new Exception("no stream");
                    os.write(data);
                }
                toast(getString(R.string.export_ok, name));
            } else {
                pendingExportId = p.id;
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2001);
                } else {
                    legacyExport(p, data);
                }
            }
        } catch (Exception e) {
            Logx.e("export", e);
            toast(getString(R.string.export_failed));
        }
    }

    private void legacyExport(Store.Project p, byte[] data) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            dir.mkdirs();
            File out = new File(dir, safeName(p.title) + ".sb3");
            Store.writeAll(out, data);
            toast(getString(R.string.export_ok, out.getName()));
        } catch (Exception e) {
            Logx.e("legacyExport", e);
            toast(getString(R.string.export_failed));
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] grant) {
        super.onRequestPermissionsResult(code, perms, grant);
        if (code == 2001 && pendingExportId != null) {
            Store.Project p = Store.get(this, pendingExportId);
            if (p != null && grant.length > 0
                    && grant[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                byte[] data = Store.readAll(p.projectFile(this));
                if (data != null) legacyExport(p, data);
            } else {
                toast(getString(R.string.export_failed));
            }
            pendingExportId = null;
        }
    }

    private void shareProject(Store.Project p) {
        File f = p.projectFile(this);
        if (!f.exists()) {
            toast(getString(R.string.export_failed));
            return;
        }
        try {
            File tmp = new File(ShareProvider.shareDir(this), safeName(p.title) + ".sb3");
            byte[] data = Store.readAll(f);
            Store.writeAll(tmp, data);
            Uri uri = ShareProvider.uriFor(tmp);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/octet-stream");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, p.title);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.action_share)));
        } catch (Exception e) {
            Logx.e("share", e);
            toast(getString(R.string.export_failed));
        }
    }

    /* ------------------------------------------------------------- importing */

    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_title)
                .setItems(new CharSequence[]{
                        getString(R.string.import_device),
                        getString(R.string.import_scratch)
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) pickProjectFile();
                        else askScratchId();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void pickProjectFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/octet-stream", "application/zip", "*/*"});
        try {
            startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            Logx.e("pick", e);
        }
    }

    private void askScratchId() {
        final EditText ed = new EditText(this);
        ed.setInputType(InputType.TYPE_CLASS_TEXT);
        ed.setHint(R.string.scratch_hint);
        int pad = Ui.dp(this, 20);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(pad, Ui.dp(this, 8), pad, 0);
        wrap.addView(ed);
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_scratch)
                .setView(wrap)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String id = scratchId(ed.getText().toString());
                        if (id == null) {
                            toast(getString(R.string.scratch_invalid));
                            return;
                        }
                        importFromScratch(id);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String scratchId(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (s.matches("[0-9]{1,12}")) return s;
        Matcher m = Pattern.compile("projects/([0-9]{1,12})").matcher(s);
        if (m.find()) return m.group(1);
        m = Pattern.compile("^/?([0-9]{1,12})/?$").matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }

    private void importFromScratch(final String id) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.importing));
        pd.setCancelable(false);
        pd.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String title = null;
                String error = null;
                try {
                    byte[] metaRaw = http("https://trampoline.turbowarp.org/api/projects/" + id);
                    JSONObject meta = new JSONObject(new String(metaRaw, "UTF-8"));
                    title = meta.optString("title", "");
                    String token = meta.optString("project_token", "");
                    String url = "https://projects.scratch.mit.edu/" + id
                            + (token.isEmpty() ? "" : "?token=" + token);
                    byte[] raw = http(url);
                    if (raw == null || raw.length < 50) throw new IOException("empty project");
                    byte[] sb3;
                    if (raw.length > 1 && raw[0] == 'P' && raw[1] == 'K') {
                        sb3 = raw;
                    } else {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ZipOutputStream zos = new ZipOutputStream(bos);
                        zos.putNextEntry(new ZipEntry("project.json"));
                        zos.write(raw);
                        zos.closeEntry();
                        zos.close();
                        sb3 = bos.toByteArray();
                    }
                    String pid = Store.newId();
                    String name = title == null || title.trim().isEmpty()
                            ? getString(R.string.default_title) : title.trim();
                    Store.Project p = Store.upsert(MainActivity.this, pid, name);
                    Store.writeAll(p.projectFile(MainActivity.this), sb3);
                } catch (Exception e) {
                    Logx.e("scratchImport", e);
                    error = e.toString();
                }
                final String fTitle = title;
                final String fError = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try { pd.dismiss(); } catch (Exception ignored) { }
                        if (fError != null) {
                            toast(getString(R.string.import_failed) + ": " + fError);
                        } else {
                            toast(getString(R.string.import_ok, fTitle == null ? id : fTitle));
                            reload();
                        }
                    }
                });
            }
        }).start();
    }

    private static byte[] http(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Mobile Safari/537.36");
        c.setRequestProperty("Accept", "*/*");
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        try {
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return bos.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_IMPORT && res == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                String name = displayName(data.getData());
                if (name != null && name.toLowerCase(Locale.US).endsWith(".sb3")) {
                    name = name.substring(0, name.length() - 4);
                }
                if (name == null || name.trim().isEmpty()) name = getString(R.string.default_title);
                String id = Store.newId();
                Store.Project p = Store.upsert(this, id, name.trim());
                Store.writeAll(p.projectFile(this), bos.toByteArray());
                reload();
                openProject(id);
            } catch (Exception e) {
                Logx.e("import", e);
                toast(getString(R.string.import_failed));
            }
        }
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
