package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.UserDao;
import com.example.furniture.model.User;
import com.example.furniture.utils.BiometricHelper;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LoginActivity — halaman login pengguna.
 * Dipanggil dari CheckoutActivity jika user belum login.
 * Setelah login berhasil, kembali ke CheckoutActivity.
 *
 * Fitur Biometric:
 *   - Jika user sudah pernah login sebelumnya (session tersimpan) dan
 *     perangkat mendukung sidik jari, prompt biometric muncul otomatis saat activity dibuka.
 *   - Jika sidik jari berhasil → langsung masuk tanpa ketik password.
 *   - Tombol sidik jari di UI juga tersedia sebagai alternatif kapan saja.
 */
public class LoginActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private ImageButton btnTogglePassword;
    private ImageButton btnFingerprint;
    private LinearLayout layoutFingerprint;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;
    private boolean passwordVisible = false;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private UserDao userDao;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initView();
        checkAndShowFingerprintOption();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView() {
        etEmail           = findViewById(R.id.et_login_email);
        etPassword        = findViewById(R.id.et_login_password);
        btnLogin          = findViewById(R.id.btn_login);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnFingerprint    = findViewById(R.id.btn_fingerprint);
        layoutFingerprint = findViewById(R.id.layout_fingerprint);
        tvGoToRegister    = findViewById(R.id.tv_go_to_register);
        progressBar       = findViewById(R.id.progress_login);

        userDao        = new UserDao(DatabaseHelper.getInstance(this));
        sessionManager = new SessionManager(this);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLogin());
        }

        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (tvGoToRegister != null) {
            tvGoToRegister.setOnClickListener(v -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivityForResult(intent, 100);
            });
        }

        if (btnFingerprint != null) {
            btnFingerprint.setOnClickListener(v -> showFingerprintPrompt());
        }
    }

    // ─── Biometric ───────────────────────────────────────────────────────────────

    /**
     * Tampilkan tombol sidik jari jika perangkat mendukung.
     * Jika user sudah pernah login sebelumnya (session tersimpan) → langsung tampilkan prompt.
     */
    private void checkAndShowFingerprintOption() {
        if (!BiometricHelper.isFingerprintAvailable(this)) return;

        // Tampilkan tombol sidik jari di UI
        if (layoutFingerprint != null) {
            layoutFingerprint.setVisibility(View.VISIBLE);
        }

        // Auto-prompt jika user sudah pernah login (ada saved email di session)
        // dan sudah pernah aktifkan fingerprint
        if (BiometricHelper.isFingerprintEnabled(this)
                && !TextUtils.isEmpty(sessionManager.getUserEmail())) {
            // Delay singkat agar UI selesai render terlebih dahulu
            if (layoutFingerprint != null) {
                layoutFingerprint.postDelayed(this::showFingerprintPrompt, 400);
            }
        }
    }

    /**
     * Tampilkan BiometricPrompt untuk sidik jari.
     * Jika berhasil → re-create session dari saved email dan langsung masuk.
     */
    private void showFingerprintPrompt() {
        BiometricHelper.showFingerprintPrompt(
                this,
                "Masuk dengan Sidik Jari",
                "Tempelkan jari Anda pada sensor sidik jari",
                new BiometricHelper.Callback() {
                    @Override
                    public void onSuccess() {
                        // Aktifkan preferensi fingerprint (sudah pernah berhasil)
                        BiometricHelper.setFingerprintEnabled(LoginActivity.this, true);

                        // Cek apakah ada sesi email tersimpan
                        String savedEmail = sessionManager.getUserEmail();

                        if (!TextUtils.isEmpty(savedEmail)) {
                            // Login ulang via DB pakai email tersimpan
                            loginByEmail(savedEmail);
                        } else {
                            // Tidak ada session → minta password manual
                            Toast.makeText(LoginActivity.this,
                                    "Masukkan email & password sekali dulu.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailed() {
                        // Sistem sudah beri feedback ke user (getar / pesan)
                        // Tidak perlu tindakan tambahan
                    }

                    @Override
                    public void onCancelled() {
                        // User memilih "Gunakan Password" — tidak perlu apa-apa
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(LoginActivity.this,
                                "Sidik jari gagal: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Login ulang menggunakan email tersimpan (dipanggil setelah biometric sukses).
     * Query ke DB untuk memastikan akun masih ada.
     */
    private void loginByEmail(String email) {
        showLoading();
        executor.execute(() -> {
            User user = userDao.getUserByEmail(email);
            runOnUiThread(() -> {
                hideLoading();
                if (user != null) {
                    sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
                    Toast.makeText(this,
                            "Selamat datang kembali, " + user.getName() + "! 👋",
                            Toast.LENGTH_SHORT).show();
                    if (isTaskRoot()) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this,
                            "Akun tidak ditemukan. Silakan login dengan password.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ─── Login Manual ────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            if (etEmail != null) etEmail.setError("Email tidak boleh kosong");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            if (etPassword != null) etPassword.setError("Password tidak boleh kosong");
            return;
        }

        showLoading();

        executor.execute(() -> {
            User user = userDao.login(email, password);

            runOnUiThread(() -> {
                hideLoading();
                if (user != null) {
                    // Login berhasil — simpan sesi
                    sessionManager.createSession(user.getId(), user.getName(), user.getEmail());

                    // Aktifkan fingerprint secara default setelah login manual berhasil
                    // (jika perangkat mendukung)
                    if (BiometricHelper.isFingerprintAvailable(this)) {
                        BiometricHelper.setFingerprintEnabled(this, true);
                    }

                    Toast.makeText(this,
                            "Selamat datang, " + user.getName() + "!",
                            Toast.LENGTH_SHORT).show();

                    if (isTaskRoot()) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this,
                            "Email atau password salah.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ─── Register Result ─────────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (isTaskRoot()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    // ─── Password Toggle ─────────────────────────────────────────────────────────

    private void togglePasswordVisibility() {
        if (etPassword == null) return;
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            if (btnTogglePassword != null) btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            if (btnTogglePassword != null) btnTogglePassword.setImageResource(R.drawable.ic_eye);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnLogin != null) btnLogin.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (btnLogin != null) btnLogin.setEnabled(true);
    }
}
