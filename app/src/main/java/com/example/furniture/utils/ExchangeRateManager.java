package com.example.furniture.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.furniture.model.ExchangeRateResponse;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Mengambil kurs USD → IDR dari exchangerate-api.com secara async.
 * Kurs terakhir berhasil diunduh disimpan di SharedPreferences sebagai cache.
 * Jika offline atau gagal, gunakan nilai cache atau fallback {@link Constants#USD_TO_IDR_FALLBACK}.
 */
public class ExchangeRateManager {

    public interface OnRateFetchedListener {
        void onSuccess(double idrRate);
        void onFailure(double fallbackRate);
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Ambil kurs dari cache SharedPreferences (sinkron, tidak hit network).
     * Cocok untuk format harga di UI saat sudah pernah fetch sebelumnya.
     */
    public static double getCachedRate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        float stored = prefs.getFloat(Constants.PREF_IDR_RATE, (float) Constants.USD_TO_IDR_FALLBACK);
        return stored > 0 ? stored : Constants.USD_TO_IDR_FALLBACK;
    }

    /**
     * Fetch kurs terbaru dari API secara async.
     * Simpan hasilnya ke SharedPreferences.
     * Panggil ini sekali saat app start (MainActivity.onCreate).
     *
     * @param context  Context aplikasi
     * @param listener Callback dengan kurs IDR hasil fetch, atau fallback jika gagal
     */
    public static void fetchRate(Context context, OnRateFetchedListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.EXCHANGE_RATE_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                double cached = getCachedRate(context);
                if (listener != null) listener.onFailure(cached);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        ExchangeRateResponse rate = new Gson().fromJson(json, ExchangeRateResponse.class);
                        double idr = rate.getIdrRate(Constants.USD_TO_IDR_FALLBACK);
                        saveRate(context, idr);
                        if (listener != null) listener.onSuccess(idr);
                    } catch (Exception e) {
                        double cached = getCachedRate(context);
                        if (listener != null) listener.onFailure(cached);
                    }
                } else {
                    double cached = getCachedRate(context);
                    if (listener != null) listener.onFailure(cached);
                }
            }
        });
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────────

    private static void saveRate(Context context, double rate) {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putFloat(Constants.PREF_IDR_RATE, (float) rate)
                .apply();
    }

    // Prevent instantiation
    private ExchangeRateManager() {}
}
