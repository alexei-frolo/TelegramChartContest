package com.froloapp.telegramchart;

import android.content.Context;
import android.content.SharedPreferences;

public final class PrefManager {

    private static volatile PrefManager instance = null;

    public static PrefManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PrefManager.class) {
                if (instance == null)
                    instance = new PrefManager(context);
            }
        }
        return instance;
    }

    private static final String STORAGE_NAME = BuildConfig.APPLICATION_ID;
    // keys
    private static final String KEY_NIGHT_MODE_ENABLED = "night_mode_enabled";

    private final SharedPreferences storage;

    private PrefManager(Context context) {
        storage = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE);
    }

    public boolean isNightModeEnabled() {
        return storage.getBoolean(KEY_NIGHT_MODE_ENABLED, false);
    }

    public void setNightModeEnabled(boolean enabled) {
        storage.edit().putBoolean(KEY_NIGHT_MODE_ENABLED, enabled).apply();
    }
}
