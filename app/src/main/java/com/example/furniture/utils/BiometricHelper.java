package com.example.furniture.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * BiometricHelper — utility untuk otentikasi sidik jari menggunakan AndroidX Biometric.
 *
 * Hanya menggunakan BIOMETRIC_STRONG (sidik jari hardware) tanpa PIN/Face fallback.
 *
 * Penggunaan:
 *   1. Cek apakah sidik jari tersedia dengan {@link #isFingerprintAvailable(Context)}.
 *   2. Tampilkan prompt dengan {@link #showFingerprintPrompt(FragmentActivity, String, String, Callback)}.
 *   3. Kelola preferensi aktif/nonaktif dengan {@link #setFingerprintEnabled} / {@link #isFingerprintEnabled}.
 */
public class BiometricHelper {

    private static final String PREF_BIOMETRIC      = "biometric_prefs";
    private static final String KEY_FINGERPRINT_ON  = "fingerprint_enabled";

    // ─── Availability Check ───────────────────────────────────────────────────────

    /**
     * Apakah perangkat mendukung dan sudah mendaftarkan sidik jari?
     *
     * @return true jika sidik jari siap digunakan
     */
    public static boolean isFingerprintAvailable(Context context) {
        BiometricManager bm = BiometricManager.from(context);
        int result = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // ─── Preference ───────────────────────────────────────────────────────────────

    /** Aktifkan atau nonaktifkan fitur sidik jari dari Settings. */
    public static void setFingerprintEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_FINGERPRINT_ON, enabled).apply();
    }

    /** Apakah user telah mengaktifkan sidik jari di Settings? */
    public static boolean isFingerprintEnabled(Context context) {
        return prefs(context).getBoolean(KEY_FINGERPRINT_ON, false);
    }

    // ─── Prompt ──────────────────────────────────────────────────────────────────

    /**
     * Tampilkan BiometricPrompt hanya untuk sidik jari.
     *
     * @param activity Activity tempat prompt ditampilkan (harus FragmentActivity)
     * @param title    Judul dialog, e.g. "Masuk dengan Sidik Jari"
     * @param subtitle Subjudul, e.g. "Tempelkan jari Anda pada sensor"
     * @param callback Callback hasil autentikasi
     */
    public static void showFingerprintPrompt(
            FragmentActivity activity,
            String title,
            String subtitle,
            Callback callback) {

        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // ERROR_NEGATIVE_BUTTON = user menekan tombol "Batal"
                        // ERROR_USER_CANCELED   = user membatalkan dari luar dialog
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            callback.onCancelled();
                        } else {
                            callback.onError(errString.toString());
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Sidik jari tidak cocok — sistem sudah memberi feedback getar/pesan
                        // Tidak perlu menutup dialog, biarkan user coba lagi
                        callback.onFailed();
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                // Hanya fingerprint (BIOMETRIC_STRONG), tanpa PIN/Pattern/Face fallback
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Gunakan Password")
                .build();

        prompt.authenticate(info);
    }

    // ─── Callback Interface ───────────────────────────────────────────────────────

    public interface Callback {
        /** Autentikasi sidik jari berhasil. */
        void onSuccess();

        /** Sidik jari tidak cocok (user masih bisa coba lagi). */
        void onFailed();

        /** User membatalkan dialog. */
        void onCancelled();

        /** Terjadi error fatal (e.g., sidik jari dikunci). */
        void onError(String errorMessage);
    }

    // ─── Private ─────────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_BIOMETRIC, Context.MODE_PRIVATE);
    }

    private BiometricHelper() {} // Prevent instantiation
}
