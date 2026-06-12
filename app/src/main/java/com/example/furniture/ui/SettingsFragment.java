package com.example.furniture.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.furniture.BuildConfig;
import com.example.furniture.R;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.ExchangeRateManager;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.SessionManager;
import com.example.furniture.utils.ThemeManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * SettingsFragment — mengatur dark/light theme, menampilkan info user,
 * menyediakan tombol logout, dan edit alamat pengiriman dengan Fused Location.
 */
public class SettingsFragment extends Fragment {

    private static final int REQUEST_LOCATION_PERM = 301;
    private static final int REQUEST_MAP_PICKER    = 302;

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
    private android.widget.RadioGroup rgLanguage;
    private android.widget.RadioButton rbLangEn;
    private android.widget.RadioButton rbLangId;

    private View layoutShippingAddress;
    private TextView tvCurrentAddress;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private SessionManager sessionManager;
    private com.example.furniture.database.UserDao userDao;
    private java.util.concurrent.ExecutorService executorService;

    // ─── Location ─────────────────────────────────────────────────────────────────

    private FusedLocationProviderClient fusedLocationClient;

    /** Referensi sementara ke dialog saat ini (untuk diisi dari callback GPS) */
    private TextInputEditText dialogEtStreet;
    private TextInputEditText dialogEtCity;
    private TextInputEditText dialogEtProvince;
    private TextInputEditText dialogEtPostalCode;
    private TextInputEditText dialogEtCountry;

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
        initLocation();
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

        // Tombol toggle bahasa
        rgLanguage = view.findViewById(R.id.rg_language);
        rbLangEn = view.findViewById(R.id.rb_lang_en);
        rbLangId = view.findViewById(R.id.rb_lang_id);
        
        updateLanguageButtonState();

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

    /**
     * Inisialisasi FusedLocationProviderClient.
     */
    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
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

        dialogEtStreet     = dialogView.findViewById(R.id.et_street);
        dialogEtCity       = dialogView.findViewById(R.id.et_city);
        dialogEtProvince   = dialogView.findViewById(R.id.et_province);
        dialogEtPostalCode = dialogView.findViewById(R.id.et_postal_code);
        dialogEtCountry    = dialogView.findViewById(R.id.et_country);

        MaterialButton btnDetectLocation = dialogView.findViewById(R.id.btn_detect_location);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_address);
        MaterialButton btnSave   = dialogView.findViewById(R.id.btn_save_address);

        // Parse existing address ke field-field
        if (tvCurrentAddress != null) {
            String current = tvCurrentAddress.getText().toString();
            if (!current.equals("Belum diatur") && !current.isEmpty()) {
                String[] parts = current.split(", ");
                if (parts.length >= 1) dialogEtStreet.setText(parts[0]);
                if (parts.length >= 2) dialogEtCity.setText(parts[1]);
                if (parts.length >= 3) dialogEtProvince.setText(parts[2]);
                if (parts.length >= 4) dialogEtPostalCode.setText(parts[3]);
                if (parts.length >= 5) {
                    StringBuilder country = new StringBuilder();
                    for (int i = 4; i < parts.length; i++) {
                        country.append(parts[i]).append(i == parts.length - 1 ? "" : ", ");
                    }
                    dialogEtCountry.setText(country.toString());
                }
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Tombol deteksi lokasi → buka Map Picker
        if (btnDetectLocation != null) {
            btnDetectLocation.setOnClickListener(v -> {
                Intent mapIntent = new Intent(requireContext(), MapPickerActivity.class);
                startActivityForResult(mapIntent, REQUEST_MAP_PICKER);
            });
        }

        btnCancel.setOnClickListener(v -> {
            clearDialogReferences();
            dialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            String street   = dialogEtStreet.getText() != null ? dialogEtStreet.getText().toString().trim() : "";
            String city     = dialogEtCity.getText() != null ? dialogEtCity.getText().toString().trim() : "";
            String province = dialogEtProvince.getText() != null ? dialogEtProvince.getText().toString().trim() : "";
            String postal   = dialogEtPostalCode.getText() != null ? dialogEtPostalCode.getText().toString().trim() : "";
            String country  = dialogEtCountry.getText() != null ? dialogEtCountry.getText().toString().trim() : "";

            if (!street.isEmpty())   sb.append(street);
            if (!city.isEmpty())     sb.append(sb.length() > 0 ? ", " : "").append(city);
            if (!province.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(province);
            if (!postal.isEmpty())   sb.append(sb.length() > 0 ? ", " : "").append(postal);
            if (!country.isEmpty())  sb.append(sb.length() > 0 ? ", " : "").append(country);

            saveAddressToDb(sb.toString());
            clearDialogReferences();
            dialog.dismiss();
        });

        dialog.show();
    }

    /** Hapus referensi dialog fields saat dialog ditutup */
    private void clearDialogReferences() {
        dialogEtStreet     = null;
        dialogEtCity       = null;
        dialogEtProvince   = null;
        dialogEtPostalCode = null;
        dialogEtCountry    = null;
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

    // ─── Fused Location ──────────────────────────────────────────────────────────

    /**
     * Minta permission lokasi lalu ambil lokasi untuk mengisi dialog.
     */
    private void requestLocationForDialog() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationForDialog();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERM);
        }
    }

    /**
     * Ambil lokasi saat ini dan isi field-field di dialog.
     */
    private void fetchLocationForDialog() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(requireContext(), "📍 Mendapatkan lokasi...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        reverseGeocodeToDialog(location.getLatitude(), location.getLongitude());
                    } else {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(lastLocation -> {
                                    if (lastLocation != null) {
                                        reverseGeocodeToDialog(
                                                lastLocation.getLatitude(),
                                                lastLocation.getLongitude());
                                    } else {
                                        Toast.makeText(requireContext(),
                                                "⚠️ Lokasi tidak dapat ditemukan. Pastikan GPS aktif.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "⚠️ Gagal mendapatkan lokasi: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Reverse geocoding dan isi field-field dialog secara terpisah.
     */
    private void reverseGeocodeToDialog(double lat, double lng) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);

                    // Ambil komponen alamat
                    String street   = buildStreetName(addr);
                    String city     = !TextUtils.isEmpty(addr.getLocality()) ? addr.getLocality()
                                    : (!TextUtils.isEmpty(addr.getSubAdminArea()) ? addr.getSubAdminArea() : "");
                    String province = !TextUtils.isEmpty(addr.getAdminArea()) ? addr.getAdminArea() : "";
                    String postal   = !TextUtils.isEmpty(addr.getPostalCode()) ? addr.getPostalCode() : "";
                    String country  = !TextUtils.isEmpty(addr.getCountryName()) ? addr.getCountryName() : "";

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Isi hanya jika dialog masih terbuka (referensi tidak null)
                            if (dialogEtStreet != null)     dialogEtStreet.setText(street);
                            if (dialogEtCity != null)       dialogEtCity.setText(city);
                            if (dialogEtProvince != null)   dialogEtProvince.setText(province);
                            if (dialogEtPostalCode != null) dialogEtPostalCode.setText(postal);
                            if (dialogEtCountry != null)    dialogEtCountry.setText(country);

                            Toast.makeText(requireContext(),
                                    "✅ Lokasi berhasil dideteksi!", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "⚠️ Alamat tidak ditemukan untuk lokasi ini.",
                                        Toast.LENGTH_LONG).show());
                    }
                }
            } catch (IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "⚠️ Geocoding gagal. Periksa koneksi internet.",
                                    Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    /**
     * Susun nama jalan dari komponen thoroughfare + subThoroughfare.
     */
    private String buildStreetName(Address addr) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(addr.getSubThoroughfare()))
            sb.append(addr.getSubThoroughfare()).append(" ");
        if (!TextUtils.isEmpty(addr.getThoroughfare()))
            sb.append(addr.getThoroughfare());
        if (sb.length() == 0 && !TextUtils.isEmpty(addr.getSubLocality()))
            sb.append(addr.getSubLocality());
        return sb.toString().trim();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationForDialog();
            } else {
                Toast.makeText(requireContext(),
                        "⚠️ Izin lokasi diperlukan untuk fitur ini.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAP_PICKER
                && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String address = data.getStringExtra(Constants.EXTRA_SELECTED_ADDRESS);
            if (address != null && !address.isEmpty()) {
                // Parse alamat dari map ke field-field dialog (jika masih terbuka)
                String[] parts = address.split(", ");
                if (dialogEtStreet != null && parts.length >= 1) dialogEtStreet.setText(parts[0]);
                if (dialogEtCity != null && parts.length >= 2) dialogEtCity.setText(parts[1]);
                if (dialogEtProvince != null && parts.length >= 3) dialogEtProvince.setText(parts[2]);
                if (dialogEtPostalCode != null && parts.length >= 4) dialogEtPostalCode.setText(parts[3]);
                if (dialogEtCountry != null && parts.length >= 5) {
                    StringBuilder country = new StringBuilder();
                    for (int i = 4; i < parts.length; i++) {
                        country.append(parts[i]).append(i == parts.length - 1 ? "" : ", ");
                    }
                    dialogEtCountry.setText(country.toString());
                }

                // Jika dialog sudah tertutup, simpan langsung ke DB
                if (dialogEtStreet == null) {
                    saveAddressToDb(address);
                }

                Toast.makeText(requireContext(), getString(R.string.location_detected),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─── Language ─────────────────────────────────────────────────────────────────

    /**
     * Simpan bahasa baru, fetch kurs terbaru jika ganti ke ID, lalu restart seluruh stack
     * ke MainActivity agar semua Activity memuat ulang locale dari awal.
     *
     * Tidak menggunakan recreate() karena Activity lain di back stack (Detail, Checkout, dll.)
     * tidak override attachBaseContext sehingga locale-nya tidak berubah dan menyebabkan crash.
     */
    private void switchLanguage(String langCode) {
        String current = LanguageManager.getLanguage(requireContext());
        if (current.equals(langCode)) return; // Tidak perlu restart jika bahasa sama

        LanguageManager.saveLanguage(requireContext(), langCode);

        // Fetch kurs terbaru saat ganti ke Indonesia, lalu restart
        if (Constants.LANG_ID.equals(langCode)) {
            ExchangeRateManager.fetchRate(requireContext(), new ExchangeRateManager.OnRateFetchedListener() {
                @Override
                public void onSuccess(double idrRate) {
                    restartApp();
                }
                @Override
                public void onFailure(double fallbackRate) {
                    restartApp();
                }
            });
        } else {
            restartApp();
        }
    }

    /**
     * Restart seluruh stack Activity ke MainActivity dengan locale baru.
     * FLAG_ACTIVITY_CLEAR_TASK memastikan semua Activity lama dihancurkan,
     * sehingga tidak ada Activity yang masih memakai locale lama.
     */
    private void restartApp() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Tidak perlu finish() — FLAG_ACTIVITY_CLEAR_TASK sudah menghancurkan semua Activity
    }

    /**
     * Perbarui tampilan tombol bahasa menggunakan RadioGroup
     */
    private void updateLanguageButtonState() {
        if (rbLangEn == null || rbLangId == null || getContext() == null) return;
        boolean isId = LanguageManager.isIndonesian(requireContext());

        if (rgLanguage != null) {
            rgLanguage.setOnCheckedChangeListener(null);
        }

        if (isId) {
            rbLangId.setChecked(true);
        } else {
            rbLangEn.setChecked(true);
        }

        if (rgLanguage != null) {
            rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_lang_en) {
                    switchLanguage(Constants.LANG_EN);
                } else if (checkedId == R.id.rb_lang_id) {
                    switchLanguage(Constants.LANG_ID);
                }
            });
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
        // AppCompatDelegate.setDefaultNightMode() will automatically recreate the Activity
    }

    private void updateThemeLabel(boolean isDarkMode) {
        if (tvThemeLabel != null) {
            tvThemeLabel.setText(isDarkMode ? "Mode Gelap" : "Mode Terang");
        }
    }
}
