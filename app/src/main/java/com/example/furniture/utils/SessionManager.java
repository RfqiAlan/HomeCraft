package com.example.furniture.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager — mengelola status login user via SharedPreferences.
 * Menyimpan userId dan data user yang sedang aktif.
 */
public class SessionManager {

    private static final String PREF_SESSION     = "furnispace_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID      = "user_id";
    private static final String KEY_USER_NAME    = "user_name";
    private static final String KEY_USER_EMAIL   = "user_email";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // ─── Login / Logout ──────────────────────────────────────────────────────────

    /**
     * Simpan sesi login user.
     */
    public void createSession(int userId, String name, String email) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    /**
     * Hapus sesi (logout).
     */
    public void logout() {
        prefs.edit().clear().apply();
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    // ─── First Time / Onboarding ────────────────────────────────────────────────

    public boolean isFirstTimeLaunch() {
        return prefs.getBoolean("is_first_time_launch", true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        prefs.edit().putBoolean("is_first_time_launch", isFirstTime).apply();
    }
}
