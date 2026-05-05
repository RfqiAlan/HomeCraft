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
 * RegisterActivity — halaman pendaftaran akun baru.
 * Setelah register berhasil, otomatis login dan kembali ke CheckoutActivity.
 */
public class RegisterActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etAddress;
    private Button btnRegister;
    private ImageButton btnTogglePassword;
    private ImageButton btnToggleConfirmPassword;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private UserDao userDao;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView() {
        etName               = findViewById(R.id.et_register_name);
        etEmail              = findViewById(R.id.et_register_email);
        etPassword           = findViewById(R.id.et_register_password);
        etConfirmPassword    = findViewById(R.id.et_register_confirm_password);
        etAddress            = findViewById(R.id.et_register_address);
        btnRegister          = findViewById(R.id.btn_register);
        btnTogglePassword    = findViewById(R.id.btn_toggle_password_reg);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password);
        tvGoToLogin          = findViewById(R.id.tv_go_to_login);
        progressBar          = findViewById(R.id.progress_register);

        userDao        = new UserDao(DatabaseHelper.getInstance(this));
        sessionManager = new SessionManager(this);

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> attemptRegister());
        }
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePassword());
        }
        if (btnToggleConfirmPassword != null) {
            btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPassword());
        }
        if (tvGoToLogin != null) {
            tvGoToLogin.setOnClickListener(v -> finish());
        }
    }

    // ─── Register ────────────────────────────────────────────────────────────────

    private void attemptRegister() {
        String name            = etName != null ? etName.getText().toString().trim() : "";
        String email           = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password        = etPassword != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";
        String address         = etAddress != null ? etAddress.getText().toString().trim() : "";

        // ─── Validasi ────────────────────────────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            if (etName != null) etName.setError("Nama tidak boleh kosong");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            if (etEmail != null) etEmail.setError("Email tidak boleh kosong");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (etEmail != null) etEmail.setError("Format email tidak valid");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            if (etPassword != null) etPassword.setError("Password tidak boleh kosong");
            return;
        }
        if (password.length() < 6) {
            if (etPassword != null) etPassword.setError("Password minimal 6 karakter");
            return;
        }
        if (!password.equals(confirmPassword)) {
            if (etConfirmPassword != null) etConfirmPassword.setError("Konfirmasi password tidak cocok");
            return;
        }

        showLoading();

        final String finalAddress = address;
        executor.execute(() -> {
            User newUser = new User(name, email, password);
            newUser.setAddress(finalAddress);

            long userId = userDao.register(newUser);

            runOnUiThread(() -> {
                hideLoading();
                if (userId != -1) {
                    // Register berhasil → langsung login
                    sessionManager.createSession((int) userId, name, email);

                    Toast.makeText(this,
                            "Akun berhasil dibuat! Selamat datang, " + name + " 🎉",
                            Toast.LENGTH_SHORT).show();

                    // Kembali ke LoginActivity dengan result OK
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this,
                            "Email sudah terdaftar. Gunakan email lain.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ─── Password Toggle ─────────────────────────────────────────────────────────

    private void togglePassword() {
        if (etPassword == null) return;
        passwordVisible = !passwordVisible;
        etPassword.setTransformationMethod(passwordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        if (btnTogglePassword != null)
            btnTogglePassword.setImageResource(passwordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        etPassword.setSelection(etPassword.getText().length());
    }

    private void toggleConfirmPassword() {
        if (etConfirmPassword == null) return;
        confirmPasswordVisible = !confirmPasswordVisible;
        etConfirmPassword.setTransformationMethod(confirmPasswordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        if (btnToggleConfirmPassword != null)
            btnToggleConfirmPassword.setImageResource(confirmPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnRegister != null) btnRegister.setEnabled(false);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (btnRegister != null) btnRegister.setEnabled(true);
    }
}
