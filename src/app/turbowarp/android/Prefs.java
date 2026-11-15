package app.turbowarp.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/** User preferences: accent colour, high contrast, language. */
public final class Prefs {

    private static final String FILE = "tw";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int accent(Context c) {
        return sp(c).getInt("accent", 0xFFFF4C4C);
    }

    public static void setAccent(Context c, int color) {
        sp(c).edit().putInt("accent", color).apply();
    }

    public static boolean highContrast(Context c) {
        return sp(c).getBoolean("hc", false);
    }

    public static void setHighContrast(Context c, boolean v) {
        sp(c).edit().putBoolean("hc", v).apply();
    }

    /** "auto" | "zh" | "en" */
    public static String language(Context c) {
        return sp(c).getString("lang", "auto");
    }

    public static void setLanguage(Context c, String v) {
        sp(c).edit().putString("lang", v).apply();
    }

    /** "auto" | "light" | "dark" */
    public static String theme(Context c) {
        return sp(c).getString("theme", "auto");
    }

    public static void setTheme(Context c, String v) {
        sp(c).edit().putString("theme", v).apply();
    }

    public static Locale forcedLocale(Context c) {
        String l = language(c);
        if ("zh".equals(l)) return Locale.SIMPLIFIED_CHINESE;
        if ("en".equals(l)) return Locale.ENGLISH;
        return null;
    }

    /** BCP-47 tag for the WebView ("zh-CN", "en-US"). */
    public static String localeTag(Context c) {
        Locale forced = forcedLocale(c);
        Locale l = forced != null ? forced : Locale.getDefault();
        String lang = l.getLanguage();
        if (lang == null || lang.isEmpty()) lang = "en";
        if ("zh".equals(lang)) return "zh-CN";
        return lang;
    }

    public static Context wrap(Context base) {
        Locale l = forcedLocale(base);
        if (l == null) return base;
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(l);
        Locale.setDefault(l);
        return base.createConfigurationContext(cfg);
    }
}
