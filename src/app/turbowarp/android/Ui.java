package app.turbowarp.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class Ui {

    /* Live theme colours - recomputed by applyTheme() from Prefs. */
    public static int ACCENT = 0xFFFF4C4C;
    public static int ACCENT_DARK = 0xFFE03A3A;
    public static int BG = 0xFFE9EEF5;
    public static int CARD = 0xFFFFFFFF;
    public static int TEXT = 0xFF575E75;
    public static int TEXT_LIGHT = 0xFF8A8F9E;
    public static int DIVIDER = 0xFFE2E6EE;
    public static boolean HIGH_CONTRAST = false;
    public static boolean DARK = false;
    public static int THUMB_BG = 0xFFF4F6FA;

    public static boolean isDark(Context c) {
        String t = Prefs.theme(c);
        if ("dark".equals(t)) return true;
        if ("light".equals(t)) return false;
        int mode = c.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyTheme(Context c) {
        ACCENT = Prefs.accent(c);
        ACCENT_DARK = darken(ACCENT, 0.78f);
        HIGH_CONTRAST = Prefs.highContrast(c);
        DARK = isDark(c);
        if (HIGH_CONTRAST) {
            BG = DARK ? 0xFF000000 : 0xFFFFFFFF;
            CARD = DARK ? 0xFF000000 : 0xFFFFFFFF;
            TEXT = DARK ? 0xFFFFFFFF : 0xFF000000;
            TEXT_LIGHT = DARK ? 0xFFE6E6E6 : 0xFF1A1A1A;
            DIVIDER = DARK ? 0xFFFFFFFF : 0xFF000000;
            THUMB_BG = DARK ? 0xFF141414 : 0xFFF4F6FA;
        } else if (DARK) {
            BG = 0xFF101215;
            CARD = 0xFF1B1E24;
            TEXT = 0xFFECEEF2;
            TEXT_LIGHT = 0xFF9AA3AE;
            DIVIDER = 0xFF2C323A;
            THUMB_BG = 0xFF14171B;
        } else {
            BG = 0xFFE9EEF5;
            CARD = 0xFFFFFFFF;
            TEXT = 0xFF575E75;
            TEXT_LIGHT = 0xFF8A8F9E;
            DIVIDER = 0xFFE2E6EE;
            THUMB_BG = 0xFFF4F6FA;
        }
    }

    public static int darken(int color, float f) {
        int r = Math.round(Color.red(color) * f);
        int g = Math.round(Color.green(color) * f);
        int b = Math.round(Color.blue(color) * f);
        return Color.rgb(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int color, float radiusDp, int strokeColor, Context c) {
        GradientDrawable d = round(color, radiusDp, c);
        d.setStroke(dp(c, HIGH_CONTRAST ? 2 : 1), strokeColor);
        return d;
    }

    public static TextView text(Context c, String s, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView iconButton(Context c, String glyph, float sizeSp) {
        TextView t = text(c, glyph, sizeSp, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        int p = dp(c, 8);
        t.setPadding(p, p, p, p);
        return t;
    }
}
