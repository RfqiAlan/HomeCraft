package com.example.furniture.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class untuk mengelola bahasa dan format mata uang.
 *
 * Aturan:
 *   - Bahasa Indonesia (id) → format harga dalam IDR (Rupiah)
 *   - Bahasa Inggris   (en) → format harga dalam USD (Dollar)
 *
 * Gunakan {@link #formatPrice(Context, double)} di semua tempat
 * yang perlu menampilkan harga, sebagai pengganti NumberFormat hardcoded.
 */
public class LanguageManager {

    // ─── Language State ───────────────────────────────────────────────────────────

    /**
     * Simpan pilihan bahasa ke SharedPreferences.
     *
     * @param context  Context aplikasi
     * @param langCode Kode bahasa: {@link Constants#LANG_EN} atau {@link Constants#LANG_ID}
     */
    public static void saveLanguage(Context context, String langCode) {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.PREF_LANGUAGE, langCode)
                .apply();
    }

    /**
     * Ambil kode bahasa yang tersimpan. Default: English.
     */
    public static String getLanguage(Context context) {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getString(Constants.PREF_LANGUAGE, Constants.LANG_EN);
    }

    /**
     * Return true jika bahasa aktif saat ini adalah Indonesia.
     */
    public static boolean isIndonesian(Context context) {
        return Constants.LANG_ID.equals(getLanguage(context));
    }

    // ─── Apply Locale ─────────────────────────────────────────────────────────────

    /**
     * Terapkan locale sesuai bahasa tersimpan ke Context.
     * Panggil di {@code attachBaseContext()} setiap Activity.
     *
     * @param context Context asli dari attachBaseContext
     * @return Context baru dengan locale yang sudah diterapkan
     */
    public static Context applyLanguage(Context context) {
        String langCode = getLanguage(context);
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
            return context;
        }
    }

    // ─── Price Formatting ─────────────────────────────────────────────────────────

    /**
     * Format harga sesuai bahasa aktif.
     *
     * - Bahasa Indonesia → konversi ke IDR menggunakan kurs cache, format: "Rp 15.000.000"
     * - Bahasa Inggris   → format USD: "$15.00"
     *
     * @param context Context untuk baca bahasa & kurs cache
     * @param usdPrice Harga dalam USD (dari API)
     * @return String harga yang sudah diformat
     */
    public static String formatPrice(Context context, double usdPrice) {
        if (isIndonesian(context)) {
            double rate   = ExchangeRateManager.getCachedRate(context);
            double idr    = usdPrice * rate;
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            return nf.format(idr);
        } else {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
            return nf.format(usdPrice);
        }
    }

    // Prevent instantiation
    private LanguageManager() {}
}
