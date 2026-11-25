package com.albarakaatulmakkiyyah.albarakaatulmakkiyyah;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;

/**
 * Remembers whether the reader should stay in portrait, stay in landscape, or rotate freely.
 */
final class OrientationLock {

    static final String PREFS = "reader_prefs";
    static final String KEY = "orientation_lock";
    static final String NONE = "none";
    static final String PORTRAIT = "portrait";
    static final String LANDSCAPE = "landscape";

    private OrientationLock() {
    }

    static String get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, NONE);
    }

    static void set(Activity activity, String mode) {
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode).apply();
        apply(activity);
    }

    static void apply(Activity activity) {
        String mode = get(activity);
        int orientation;
        if (PORTRAIT.equals(mode)) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        } else if (LANDSCAPE.equals(mode)) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
        activity.setRequestedOrientation(orientation);
    }
}
