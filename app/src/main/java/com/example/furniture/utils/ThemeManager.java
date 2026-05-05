package com.example.furniture.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class untuk mengatur dark/light theme.
 * Tema disimpan di SharedPreferences dan diterapkan via AppCompatDelegate.
 */
public class ThemeManager {

    /**
     * Menyimpan pilihan tema ke SharedPreferences.
     *
     * @param context    Context aplikasi
     * @param isDarkMode true = dark mode, false = light mode
     */
    public static void saveTheme(Context context, boolean isDarkMode) {
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_THEME_MODE,
                        isDarkMode ? Constants.THEME_DARK : Constants.THEME_LIGHT)
                .apply();
    }

    /**
     * Mengecek apakah tema saat ini adalah dark mode.
     *
     * @param context Context aplikasi
     * @return true jika dark mode aktif
     */
    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        String theme = prefs.getString(Constants.PREF_THEME_MODE, Constants.THEME_LIGHT);
        return Constants.THEME_DARK.equals(theme);
    }

    /**
     * Menerapkan tema yang tersimpan ke seluruh aplikasi.
     * Panggil ini di onCreate() sebelum setContentView().
     *
     * @param context Context aplikasi
     */
    public static void applyTheme(Context context) {
        if (isDarkMode(context)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Prevent instantiation
    private ThemeManager() {}
}
