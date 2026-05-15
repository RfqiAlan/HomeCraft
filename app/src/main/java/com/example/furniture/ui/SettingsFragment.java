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
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_address, null);
        
        com.google.android.material.textfield.TextInputEditText etStreet = dialogView.findViewById(R.id.et_street);
        com.google.android.material.textfield.TextInputEditText etCity = dialogView.findViewById(R.id.et_city);
        com.google.android.material.textfield.TextInputEditText etProvince = dialogView.findViewById(R.id.et_province);
        com.google.android.material.textfield.TextInputEditText etPostalCode = dialogView.findViewById(R.id.et_postal_code);
        com.google.android.material.textfield.TextInputEditText etCountry = dialogView.findViewById(R.id.et_country);
        
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_address);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_address);

        // Parse existing address
        if (tvCurrentAddress != null) {
            String current = tvCurrentAddress.getText().toString();
            if (!current.equals("Belum diatur") && !current.isEmpty()) {
                String[] parts = current.split(", ");
                if (parts.length >= 1) etStreet.setText(parts[0]);
                if (parts.length >= 2) etCity.setText(parts[1]);
                if (parts.length >= 3) etProvince.setText(parts[2]);
                if (parts.length >= 4) etPostalCode.setText(parts[3]);
                if (parts.length >= 5) {
                    StringBuilder country = new StringBuilder();
                    for(int i = 4; i < parts.length; i++) {
                        country.append(parts[i]).append(i == parts.length - 1 ? "" : ", ");
                    }
                    etCountry.setText(country.toString());
                }
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
                
        // Style dialog to be transparent so our layout corners show (if any)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            String street = etStreet.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String province = etProvince.getText().toString().trim();
            String postal = etPostalCode.getText().toString().trim();
            String country = etCountry.getText().toString().trim();
            
            if (!street.isEmpty()) sb.append(street);
            if (!city.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(city);
            if (!province.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(province);
            if (!postal.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(postal);
            if (!country.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(country);

            saveAddressToDb(sb.toString());
            dialog.dismiss();
        });

        dialog.show();
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
