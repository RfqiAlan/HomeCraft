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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.UserDao;
import com.example.furniture.model.User;
import com.example.furniture.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LoginActivity — halaman login pengguna.
 * Dipanggil dari CheckoutActivity jika user belum login.
 * Setelah login berhasil, kembali ke CheckoutActivity.
 */
public class LoginActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private ImageButton btnTogglePassword;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;
    private boolean passwordVisible = false;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private UserDao userDao;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Sembunyikan ActionBar agar tampil seperti mockup
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView() {
        etEmail          = findViewById(R.id.et_login_email);
        etPassword       = findViewById(R.id.et_login_password);
        btnLogin         = findViewById(R.id.btn_login);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        tvGoToRegister   = findViewById(R.id.tv_go_to_register);
        progressBar      = findViewById(R.id.progress_login);

        userDao        = new UserDao(DatabaseHelper.getInstance(this));
        sessionManager = new SessionManager(this);

        // Tombol login
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLogin());
        }

        // Toggle password visibility
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        // Link ke halaman register
        if (tvGoToRegister != null) {
            tvGoToRegister.setOnClickListener(v -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivityForResult(intent, 100);
            });
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        // Validasi input
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
                    Toast.makeText(this,
                            "Selamat datang, " + user.getName() + "!",
                            Toast.LENGTH_SHORT).show();

                    // Jika dipanggil dari flow Checkout, kirim RESULT_OK saja
                    // Jika dipanggil dari Boarding (tidak ada caller), buka MainActivity
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

    // ─── Hasil dari RegisterActivity ─────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Jika register berhasil → langsung buka MainActivity
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
        // Jaga cursor tetap di akhir
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
