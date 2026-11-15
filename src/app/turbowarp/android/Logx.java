package app.turbowarp.android;

import android.util.Log;

public class Logx {
    public static final String TAG = "TWAndroid";

    public static void d(String m) { Log.d(TAG, m); }
    public static void i(String m) { Log.i(TAG, m); }
    public static void w(String m) { Log.w(TAG, m); }

    public static void e(String where, Throwable t) {
        Log.e(TAG, where + ": " + (t == null ? "?" : t.toString()));
        if (t != null) Log.e(TAG, Log.getStackTraceString(t));
    }
}
