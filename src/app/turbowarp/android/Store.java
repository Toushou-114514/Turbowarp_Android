package app.turbowarp.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Project storage: one folder per project under filesDir/projects, plus an index file. */
public class Store {

    public static class Project {
        public String id;
        public String title;
        public long created;
        public long modified;

        public File dir(Context c) { return new File(root(c), id); }
        public File projectFile(Context c) { return new File(dir(c), "project.sb3"); }
        public File thumbFile(Context c) { return new File(dir(c), "thumb.png"); }
    }

    private static final String INDEX = "index.json";
    private static final Object LOCK = new Object();

    public static File root(Context c) {
        File f = new File(c.getFilesDir(), "projects");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public static String newId() {
        return "p" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }

    public static List<Project> list(Context c) {
        synchronized (LOCK) {
            List<Project> out = new ArrayList<>();
            try {
                File f = new File(root(c), INDEX);
                if (!f.exists()) return out;
                String s = readText(f);
                JSONArray arr = new JSONArray(s);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Project p = new Project();
                    p.id = o.getString("id");
                    p.title = o.optString("title", "Untitled");
                    p.created = o.optLong("created", 0);
                    p.modified = o.optLong("modified", 0);
                    if (!p.projectFile(c).exists()) continue;
                    out.add(p);
                }
            } catch (Exception e) {
                Logx.e("Store.list", e);
            }
            // newest first
            java.util.Collections.sort(out, new java.util.Comparator<Project>() {
                @Override
                public int compare(Project a, Project b) {
                    return Long.compare(b.modified, a.modified);
                }
            });
            return out;
        }
    }

    private static void saveIndex(Context c, List<Project> items) {
        try {
            JSONArray arr = new JSONArray();
            for (Project p : items) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("title", p.title);
                o.put("created", p.created);
                o.put("modified", p.modified);
                arr.put(o);
            }
            File f = new File(root(c), INDEX);
            writeText(f, arr.toString());
        } catch (Exception e) {
            Logx.e("Store.saveIndex", e);
        }
    }

    public static Project get(Context c, String id) {
        for (Project p : list(c)) if (p.id.equals(id)) return p;
        return null;
    }

    /** Insert or update metadata; keeps ordering by modified time. */
    public static Project upsert(Context c, String id, String title) {
        synchronized (LOCK) {
            List<Project> items = list(c);
            Project found = null;
            for (Project p : items) if (p.id.equals(id)) { found = p; break; }
            if (found == null) {
                found = new Project();
                found.id = id;
                found.created = System.currentTimeMillis();
                found.title = title;
                items.add(found);
            }
            if (title != null && title.length() > 0) found.title = title;
            found.modified = System.currentTimeMillis();
            found.dir(c).mkdirs();
            saveIndex(c, items);
            return found;
        }
    }

    public static void rename(Context c, String id, String title) {
        synchronized (LOCK) {
            List<Project> items = list(c);
            for (Project p : items) {
                if (p.id.equals(id)) {
                    p.title = title;
                    p.modified = System.currentTimeMillis();
                }
            }
            saveIndex(c, items);
        }
    }

    public static void delete(Context c, String id) {
        synchronized (LOCK) {
            List<Project> items = list(c);
            List<Project> keep = new ArrayList<>();
            for (Project p : items) {
                if (p.id.equals(id)) deleteTree(p.dir(c));
                else keep.add(p);
            }
            saveIndex(c, keep);
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }

    public static byte[] readAll(File f) {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            Logx.e("readAll", e);
            return null;
        }
    }

    public static void writeAll(File f, byte[] data) {
        try {
            f.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(data);
            }
        } catch (Exception e) {
            Logx.e("writeAll", e);
        }
    }

    static String readText(File f) throws Exception {
        byte[] b = readAll(f);
        return b == null ? "" : new String(b, "UTF-8");
    }

    static void writeText(File f, String s) throws Exception {
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(s.getBytes("UTF-8"));
        }
    }

    /** Remember which project the editor should reopen. */
    public static void setLastProject(Context c, String id) {
        SharedPreferences sp = c.getSharedPreferences("state", Context.MODE_PRIVATE);
        sp.edit().putString("last", id).apply();
    }

    public static String getLastProject(Context c) {
        SharedPreferences sp = c.getSharedPreferences("state", Context.MODE_PRIVATE);
        return sp.getString("last", null);
    }
}
