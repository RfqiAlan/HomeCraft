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
    private TextView tvUserInfo;
    private Button btnLogout;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private SessionManager sessionManager;

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

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        switchTheme  = view.findViewById(R.id.switch_dark_mode);
        tvThemeLabel = view.findViewById(R.id.tv_theme_label);
        tvAppVersion = view.findViewById(R.id.tv_app_version);
        tvUserInfo   = view.findViewById(R.id.tv_user_info);
        btnLogout    = view.findViewById(R.id.btn_logout);

        sessionManager = new SessionManager(requireContext());

        // Tampilkan versi aplikasi
        if (tvAppVersion != null) {
            tvAppVersion.setText("FurniSpace v" + BuildConfig.VERSION_NAME);
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
    }

    // ─── User Info ───────────────────────────────────────────────────────────────

    /**
     * Tampilkan info user yang sedang login, atau pesan jika belum login.
     */
    private void displayUserInfo() {
        if (tvUserInfo == null) return;

        if (sessionManager.isLoggedIn()) {
            String info = "👤 " + sessionManager.getUserName()
                    + "\n📧 " + sessionManager.getUserEmail();
            tvUserInfo.setText(info);
            if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
        } else {
            tvUserInfo.setText("Belum login. Login diperlukan saat checkout.");
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
        }
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
