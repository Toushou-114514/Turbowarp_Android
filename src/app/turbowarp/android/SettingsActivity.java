package app.turbowarp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private static final int[] ACCENTS = {
            0xFFFF4C4C, // TurboWarp red
            0xFFFF8C1A, // orange
            0xFF2FB65A, // green
            0xFF17A2B8, // teal
            0xFF2F6FED, // blue
            0xFF8E44AD, // purple
            0xFFE84393, // pink
            0xFF343A46  // graphite
    };

    private static final String AUTHOR_URL = "https://github.com/Toushou-114514";
    private static final String REPO_URL = "https://github.com/Toushou-114514/Turbowarp_Android";

    private LinearLayout body;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Prefs.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyTheme(this);
        if (Ui.DARK) setTheme(R.style.AppTheme_Dark);
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

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 18);
        body.setPadding(pad, Ui.dp(this, 14), pad, Ui.dp(this, 30));
        sv.addView(body);
        col.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        buildBody();
        setContentView(root);
    }

    private View buildTopBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(Ui.ACCENT);
        bar.setElevation(Ui.dp(this, 4));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));

        TextView back = Ui.text(this, "\u2190", 22, Color.WHITE, false);
        back.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        bar.addView(back, bp);

        TextView title = Ui.text(this, getString(R.string.settings_title), 18, Color.WHITE, true);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.CENTER;
        bar.addView(title, tp);
        return bar;
    }

    private void buildBody() {
        body.removeAllViews();

        body.addView(sectionLabel(getString(R.string.section_appearance)));
        body.addView(card(accentRow()));
        body.addView(card(modeRows()));
        body.addView(card(highContrastRow()));

        body.addView(sectionLabel(getString(R.string.section_language)));
        body.addView(card(languageRows()));

        body.addView(sectionLabel(getString(R.string.section_about)));
        body.addView(card(aboutBlock()));
    }

    private TextView sectionLabel(String s) {
        TextView t = Ui.text(this, s, 13, Ui.TEXT_LIGHT, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, 18);
        p.bottomMargin = Ui.dp(this, 8);
        p.leftMargin = Ui.dp(this, 4);
        t.setLayoutParams(p);
        return t;
    }

    private View card(View content) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.roundStroke(Ui.CARD, 14, Ui.DIVIDER, this));
        c.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        c.addView(content);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = Ui.dp(this, 10);
        c.setLayoutParams(p);
        return c;
    }

    /* ------------------------------------------------------------ appearance */

    private View accentRow() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView label = Ui.text(this, getString(R.string.theme_color), 15, Ui.TEXT, true);
        box.addView(label);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = Ui.dp(this, 12);
        row.setLayoutParams(rp);

        final int current = Prefs.accent(this);
        int size = Ui.dp(this, 40);
        int gap = Ui.dp(this, 8);
        boolean isPreset = false;
        for (int c : ACCENTS) if (c == current) isPreset = true;

        for (final int color : ACCENTS) {
            row.addView(swatch(color, color == current, size, gap));
        }

        // custom swatch: shows the current colour when it is not one of the presets
        FrameLayout custom = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.rightMargin = gap;
        custom.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (!isPreset) {
            bg.setColor(current);
            bg.setStroke(Ui.dp(this, 3), Ui.HIGH_CONTRAST ? 0xFF000000 : Ui.TEXT);
        } else {
            bg.setColor(Ui.DIVIDER);
        }
        custom.setBackground(bg);
        TextView glyph = Ui.text(this, isPreset ? "+" : "\u2713", 17,
                isPreset ? Ui.TEXT : Color.WHITE, true);
        glyph.setGravity(Gravity.CENTER);
        custom.addView(glyph, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        custom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { pickCustomColor(); }
        });
        row.addView(custom);

        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        box.addView(hs);
        return box;
    }

    private View swatch(final int color, boolean selected, int size, int gap) {
        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.rightMargin = gap;
        wrap.setLayoutParams(lp);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        if (selected) circle.setStroke(Ui.dp(this, 3), Ui.HIGH_CONTRAST ? 0xFF000000 : Ui.TEXT);
        wrap.setBackground(circle);
        if (selected) {
            TextView check = Ui.text(this, "\u2713", 15, Color.WHITE, true);
            check.setGravity(Gravity.CENTER);
            wrap.addView(check, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        wrap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setAccent(SettingsActivity.this, color);
                recreate();
            }
        });
        return wrap;
    }

    private void pickCustomColor() {
        final int current = Prefs.accent(this);
        final int[] rgb = {Color.red(current), Color.green(current), Color.blue(current)};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 16), p, 0);

        final GradientDrawable previewBg = new GradientDrawable();
        previewBg.setShape(GradientDrawable.OVAL);
        previewBg.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
        previewBg.setStroke(Ui.dp(this, 2), Ui.DIVIDER);
        View preview = new View(this);
        preview.setBackground(previewBg);
        int ps = Ui.dp(this, 56);
        box.addView(preview, new LinearLayout.LayoutParams(ps, ps));

        final TextView hex = Ui.text(this, hexOf(rgb), 13, Ui.TEXT_LIGHT, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = Ui.dp(this, 8);
        hp.bottomMargin = Ui.dp(this, 6);
        box.addView(hex, hp);

        final String[] names = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = Ui.dp(this, 6);
            row.setLayoutParams(rowLp);

            TextView tag = Ui.text(this, names[i], 14, Ui.TEXT, true);
            row.addView(tag, new LinearLayout.LayoutParams(Ui.dp(this, 20),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            SeekBar bar = new SeekBar(this);
            bar.setMax(255);
            bar.setProgress(rgb[i]);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                    rgb[idx] = value;
                    previewBg.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                    hex.setText(hexOf(rgb));
                }

                @Override public void onStartTrackingTouch(SeekBar sb) { }

                @Override public void onStopTrackingTouch(SeekBar sb) { }
            });
            row.addView(bar, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            box.addView(row);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.custom_color)
                .setView(box)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Prefs.setAccent(SettingsActivity.this, Color.rgb(rgb[0], rgb[1], rgb[2]));
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String hexOf(int[] rgb) {
        return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
    }

    private View modeRows() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView label = Ui.text(this, getString(R.string.dark_mode), 15, Ui.TEXT, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 12);
        label.setLayoutParams(lp);
        box.addView(label);

        String cur = Prefs.theme(this);
        box.addView(optionRow("auto", getString(R.string.mode_auto), "auto".equals(cur), true));
        box.addView(optionRow("light", getString(R.string.mode_light), "light".equals(cur), true));
        box.addView(optionRow("dark", getString(R.string.mode_dark), "dark".equals(cur), true));
        return box;
    }

    private View highContrastRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 8);
        row.setLayoutParams(rlp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, getString(R.string.high_contrast), 15, Ui.TEXT, true));
        TextView hint = Ui.text(this, getString(R.string.high_contrast_hint), 12, Ui.TEXT_LIGHT, false);
        hint.setPadding(0, Ui.dp(this, 4), 0, 0);
        texts.addView(hint);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(Prefs.highContrast(this));
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                Prefs.setHighContrast(SettingsActivity.this, checked);
                recreate();
            }
        });
        row.addView(sw);
        return row;
    }

    /* ---------------------------------------------------------------- language */

    private View languageRows() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        String cur = Prefs.language(this);
        box.addView(optionRow("auto", getString(R.string.lang_auto), "auto".equals(cur), false));
        box.addView(optionRow("zh", getString(R.string.lang_zh), "zh".equals(cur), false));
        box.addView(optionRow("en", getString(R.string.lang_en), "en".equals(cur), false));
        return box;
    }

    private View optionRow(final String value, String label, boolean selected,
                           final boolean isTheme) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int v = Ui.dp(this, 11);
        row.setPadding(0, v, 0, v);

        row.addView(Ui.text(this, label, 15, Ui.TEXT, selected),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (selected) {
            row.addView(Ui.text(this, "\u2713", 16, Ui.ACCENT, true));
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTheme) Prefs.setTheme(SettingsActivity.this, value);
                else Prefs.setLanguage(SettingsActivity.this, value);
                recreate();
            }
        });
        return row;
    }

    /* ------------------------------------------------------------------- about */

    private View aboutBlock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        int s = Ui.dp(this, 44);
        head.addView(icon, new LinearLayout.LayoutParams(s, s));
        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setPadding(Ui.dp(this, 12), 0, 0, 0);
        nameBox.addView(Ui.text(this, getString(R.string.app_name), 16, Ui.TEXT, true));
        nameBox.addView(Ui.text(this, versionName(), 12, Ui.TEXT_LIGHT, false));
        head.addView(nameBox);
        box.addView(head);

        box.addView(linkBlock(getString(R.string.author_label), "Toushou", AUTHOR_URL));
        box.addView(linkBlock(getString(R.string.repo_label), REPO_URL, REPO_URL));

        TextView about = Ui.text(this, getString(R.string.about_text), 13, Ui.TEXT_LIGHT, false);
        about.setLineSpacing(Ui.dp(this, 3), 1f);
        about.setPadding(0, Ui.dp(this, 16), 0, 0);
        box.addView(about);
        return box;
    }

    private View linkBlock(String label, String text, final String url) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14);
        box.setLayoutParams(lp);

        box.addView(Ui.text(this, label, 12, Ui.TEXT_LIGHT, false));
        TextView link = Ui.text(this, text, 14, Ui.ACCENT, true);
        link.setPadding(0, Ui.dp(this, 3), 0, 0);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
            }
        });
        box.addView(link);
        return box;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
