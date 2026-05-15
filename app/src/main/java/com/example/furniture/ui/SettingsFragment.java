package com.example.furniture.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.furniture.BuildConfig;
import com.example.furniture.R;
import com.example.furniture.utils.SessionManager;
import com.example.furniture.utils.ThemeManager;

/**
 * SettingsFragment — mengatur dark/light theme, menampilkan info user,
 * dan menyediakan tombol logout.
 */
public class SettingsFragment extends Fragment {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private Switch switchTheme;
    private TextView tvThemeLabel;
    private TextView tvAppVersion;
    private View layoutAuthenticated;
    private View layoutUnauthenticated;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private Button btnLogout;
    private Button btnLogin;
    
    private View layoutShippingAddress;
    private TextView tvCurrentAddress;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private SessionManager sessionManager;
    private com.example.furniture.database.UserDao userDao;
    private java.util.concurrent.ExecutorService executorService;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        loadSavedTheme();
        displayUserInfo();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        switchTheme  = view.findViewById(R.id.switch_dark_mode);
        tvThemeLabel = view.findViewById(R.id.tv_theme_label);
        tvAppVersion = view.findViewById(R.id.tv_app_version);
        layoutAuthenticated = view.findViewById(R.id.layout_authenticated);
        layoutUnauthenticated = view.findViewById(R.id.layout_unauthenticated);
        tvUserName   = view.findViewById(R.id.tv_user_name);
        tvUserEmail  = view.findViewById(R.id.tv_user_email);
        btnLogout    = view.findViewById(R.id.btn_logout);
        btnLogin     = view.findViewById(R.id.btn_login_profile);
        layoutShippingAddress = view.findViewById(R.id.layout_shipping_address);
        tvCurrentAddress      = view.findViewById(R.id.tv_current_address);

        sessionManager = new SessionManager(requireContext());
        userDao = new com.example.furniture.database.UserDao(
            com.example.furniture.database.DatabaseHelper.getInstance(requireContext())
        );
        executorService = java.util.concurrent.Executors.newSingleThreadExecutor();

        // Tampilkan versi aplikasi
        if (tvAppVersion != null) {
            tvAppVersion.setText("HomeCraft v" + BuildConfig.VERSION_NAME);
        }

        // Listener toggle tema
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveTheme(isChecked);
                applyTheme();
                updateThemeLabel(isChecked);
            });
        }

        // Tombol logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }

        // Tombol login
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });
        }
        
        // Edit Shipping Address
        if (layoutShippingAddress != null) {
            layoutShippingAddress.setOnClickListener(v -> showEditAddressDialog());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserInfo(); // Refresh user info if they logged in from LoginActivity
    }

    // ─── User Info & Address ──────────────────────────────────────────────────────

    /**
     * Tampilkan info user yang sedang login, atau pesan jika belum login.
     */
    private void displayUserInfo() {
        if (sessionManager == null) return;

        if (sessionManager.isLoggedIn()) {
            if (layoutAuthenticated != null) layoutAuthenticated.setVisibility(View.VISIBLE);
            if (layoutUnauthenticated != null) layoutUnauthenticated.setVisibility(View.GONE);

            if (tvUserName != null) tvUserName.setText(sessionManager.getUserName());
            if (tvUserEmail != null) tvUserEmail.setText(sessionManager.getUserEmail());
            
            // Load address from DB
            loadAddressFromDb();
        } else {
            if (layoutAuthenticated != null) layoutAuthenticated.setVisibility(View.GONE);
            if (layoutUnauthenticated != null) layoutUnauthenticated.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadAddressFromDb() {
        executorService.execute(() -> {
            com.example.furniture.model.User user = userDao.getUserById(sessionManager.getUserId());
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (tvCurrentAddress != null) {
                        String address = user.getAddress();
                        tvCurrentAddress.setText((address != null && !address.trim().isEmpty()) ? address : "Belum diatur");
                    }
                });
            }
        });
    }
    
    private void showEditAddressDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        if (tvCurrentAddress != null && !tvCurrentAddress.getText().toString().equals("Belum diatur")) {
            input.setText(tvCurrentAddress.getText().toString());
        }
        input.setHint("Masukkan alamat pengiriman");

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Alamat")
                .setView(input)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newAddress = input.getText().toString().trim();
                    saveAddressToDb(newAddress);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
    
    private void saveAddressToDb(String newAddress) {
        executorService.execute(() -> {
            userDao.updateAddress(sessionManager.getUserId(), newAddress);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (tvCurrentAddress != null) {
                        tvCurrentAddress.setText(newAddress.isEmpty() ? "Belum diatur" : newAddress);
                    }
                    Toast.makeText(requireContext(), "Alamat diperbarui", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Logout: hapus sesi dan refresh tampilan.
     */
    private void logout() {
        sessionManager.logout();
        displayUserInfo();
        Toast.makeText(requireContext(), "Berhasil logout.", Toast.LENGTH_SHORT).show();
    }

    // ─── Theme ───────────────────────────────────────────────────────────────────

    private void loadSavedTheme() {
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener(null);
            switchTheme.setChecked(isDark);
            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveTheme(isChecked);
                applyTheme();
                updateThemeLabel(isChecked);
            });
        }
        updateThemeLabel(isDark);
    }

    private void saveTheme(boolean isDarkMode) {
        ThemeManager.saveTheme(requireContext(), isDarkMode);
    }

    private void applyTheme() {
        ThemeManager.applyTheme(requireContext());
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void updateThemeLabel(boolean isDarkMode) {
        if (tvThemeLabel != null) {
            tvThemeLabel.setText(isDarkMode ? "Mode Gelap" : "Mode Terang");
        }
    }
}
