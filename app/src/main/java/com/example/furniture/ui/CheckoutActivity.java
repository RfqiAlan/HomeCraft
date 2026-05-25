package com.example.furniture.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.database.CartDao;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.OrderDao;
import com.example.furniture.database.UserDao;
import com.example.furniture.model.CartItem;
import com.example.furniture.model.Order;
import com.example.furniture.model.User;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CheckoutActivity — ringkasan pesanan, cek login, load profil user,
 * pilih payment method, simpan alamat, dan proses order.
 *
 * Alur:
 *   1. Cek apakah user sudah login via SessionManager
 *   2. Jika belum → redirect ke LoginActivity
 *   3. Jika sudah → load profil user (nama, alamat, payment default)
 *   4. Tampilkan form checkout dengan data yang sudah tersimpan
 *   5. Setelah order → update profil user dengan alamat & payment terbaru
 */
public class CheckoutActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN          = 200;
    private static final int REQUEST_LOCATION_PERM  = 201;

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView rvCheckoutItems;
    private TextView tvCheckoutTotal;
    private TextView tvUserName;
    private EditText etShippingAddress;
    private EditText etPhoneNumber;
    private RadioGroup rgPaymentMethod;
    private Button btnPlaceOrder;
    private View layoutSuccess;
    private View layoutCheckout;
    private ImageButton btnUseLocationCheckout;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private CartDao cartDao;
    private OrderDao orderDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<CartItem> cartItems;
    private double totalPrice = 0.0;
    private User currentUser;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── NFC ──────────────────────────────────────────────────────────────────────
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private AlertDialog nfcWaitingDialog;

    // ─── Location ────────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Checkout");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initView();
        initNfc();
        initLocation();

        // ─── Cek login sebelum lanjut ──────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        } else {
            loadUserAndCheckout();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Aktifkan NFC foreground dispatch agar activity ini prioritas terima intent NFC
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Matikan NFC foreground dispatch saat activity tidak aktif
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Dipanggil saat NFC tag/kartu ditempel
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            handleNfcTap();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (nfcWaitingDialog != null && nfcWaitingDialog.isShowing()) {
            nfcWaitingDialog.dismiss();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView() {
        rvCheckoutItems       = findViewById(R.id.rv_checkout_items);
        tvCheckoutTotal       = findViewById(R.id.tv_checkout_total);
        tvUserName            = findViewById(R.id.tv_checkout_user_name);
        etShippingAddress     = findViewById(R.id.et_shipping_address);
        etPhoneNumber         = findViewById(R.id.et_phone_number);
        rgPaymentMethod       = findViewById(R.id.rg_payment_method);
        btnPlaceOrder         = findViewById(R.id.btn_place_order);
        layoutSuccess         = findViewById(R.id.layout_order_success);
        layoutCheckout        = findViewById(R.id.layout_checkout_content);
        btnUseLocationCheckout = findViewById(R.id.btn_use_location_checkout);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        cartDao        = new CartDao(dbHelper);
        orderDao       = new OrderDao(dbHelper);
        userDao        = new UserDao(dbHelper);
        sessionManager = new SessionManager(this);

        if (rvCheckoutItems != null) {
            rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnPlaceOrder != null) {
            btnPlaceOrder.setOnClickListener(v -> placeOrder());
        }

        // Tombol GPS untuk isi alamat otomatis
        if (btnUseLocationCheckout != null) {
            btnUseLocationCheckout.setOnClickListener(v -> requestLocationAndFill());
        }

        setupPaymentMethods();
    }

    /**
     * Inisialisasi FusedLocationProviderClient.
     */
    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Isi RadioGroup dengan daftar metode pembayaran dari Constants.
     */
    private void setupPaymentMethods() {
        if (rgPaymentMethod == null) return;
        for (String method : Constants.PAYMENT_METHODS) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(method);
            rb.setPadding(8, 16, 8, 16);
            rgPaymentMethod.addView(rb);
        }

        // Saat NFC Payment dipilih, tampilkan panduan
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null && "NFC Payment".equals(selected.getText().toString())) {
                showNfcGuide();
            }
        });
    }

    // ─── Fused Location ──────────────────────────────────────────────────────────

    /**
     * Minta permission lokasi lalu ambil lokasi jika sudah diberikan.
     */
    private void requestLocationAndFill() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERM);
        }
    }

    /**
     * Ambil lokasi terakhir yang diketahui, lalu reverse geocoding ke alamat.
     */
    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "📍 Mendapatkan lokasi...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        reverseGeocodeAndFillCheckout(location.getLatitude(), location.getLongitude());
                    } else {
                        // Fallback ke getLastLocation jika getCurrentLocation kosong
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, lastLocation -> {
                                    if (lastLocation != null) {
                                        reverseGeocodeAndFillCheckout(
                                                lastLocation.getLatitude(),
                                                lastLocation.getLongitude());
                                    } else {
                                        Toast.makeText(this,
                                                "⚠️ Lokasi tidak dapat ditemukan. Pastikan GPS aktif.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this,
                                "⚠️ Gagal mendapatkan lokasi: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Reverse geocoding: koordinat → alamat teks → isi EditText.
     */
    private void reverseGeocodeAndFillCheckout(double lat, double lng) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();

                    // Susun alamat: jalan, kecamatan, kota, provinsi, kode pos, negara
                    if (!TextUtils.isEmpty(addr.getThoroughfare()))
                        sb.append(addr.getThoroughfare());
                    if (!TextUtils.isEmpty(addr.getSubLocality()))
                        sb.append(sb.length() > 0 ? ", " : "").append(addr.getSubLocality());
                    if (!TextUtils.isEmpty(addr.getLocality()))
                        sb.append(sb.length() > 0 ? ", " : "").append(addr.getLocality());
                    if (!TextUtils.isEmpty(addr.getAdminArea()))
                        sb.append(sb.length() > 0 ? ", " : "").append(addr.getAdminArea());
                    if (!TextUtils.isEmpty(addr.getPostalCode()))
                        sb.append(sb.length() > 0 ? ", " : "").append(addr.getPostalCode());
                    if (!TextUtils.isEmpty(addr.getCountryName()))
                        sb.append(sb.length() > 0 ? ", " : "").append(addr.getCountryName());

                    String fullAddress = sb.toString();
                    runOnUiThread(() -> {
                        if (etShippingAddress != null && !fullAddress.isEmpty()) {
                            etShippingAddress.setText(fullAddress);
                            etShippingAddress.setError(null);
                            Toast.makeText(this, "✅ Alamat berhasil diisi dari lokasi GPS",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "⚠️ Alamat tidak ditemukan untuk lokasi ini.",
                                    Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "⚠️ Geocoding gagal. Periksa koneksi internet.",
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this,
                        "⚠️ Izin lokasi diperlukan untuk fitur ini.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─── NFC ─────────────────────────────────────────────────────────────────────

    /**
     * Inisialisasi NfcAdapter dan PendingIntent untuk foreground dispatch.
     */
    private void initNfc() {
        NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
        if (nfcManager != null) {
            nfcAdapter = nfcManager.getDefaultAdapter();
        }

        Intent nfcIntent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, flags);
    }

    /**
     * Tampilkan informasi panduan NFC ketika metode NFC Payment dipilih.
     */
    private void showNfcGuide() {
        if (nfcAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("NFC Tidak Tersedia")
                    .setMessage("Perangkat ini tidak mendukung NFC. Silakan pilih metode pembayaran lain.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("NFC Tidak Aktif")
                    .setMessage("Aktifkan NFC di pengaturan perangkat untuk menggunakan metode pembayaran ini.")
                    .setPositiveButton("Buka Pengaturan", (d, w) -> startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS)))
                    .setNegativeButton("Batal", null)
                    .show();
            return;
        }
        Toast.makeText(this, "✅ NFC aktif! Tap kartu saat menekan Bayar.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Tampilkan dialog menunggu tap NFC, lalu proses order.
     */
    private void showNfcWaitingDialog() {
        nfcWaitingDialog = new AlertDialog.Builder(this)
                .setTitle("💳 NFC Payment")
                .setMessage("Tempelkan kartu atau perangkat NFC Anda ke belakang ponsel...")
                .setCancelable(true)
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .create();
        nfcWaitingDialog.show();
    }

    /**
     * Dipanggil saat NFC tag terdeteksi di onNewIntent.
     * Tutup dialog tunggu dan proses order.
     */
    private void handleNfcTap() {
        if (nfcWaitingDialog != null && nfcWaitingDialog.isShowing()) {
            nfcWaitingDialog.dismiss();
        }
        // Validasi form terlebih dahulu sebelum proses
        if (!validateForm()) return;
        Toast.makeText(this, "✅ Kartu terdeteksi! Memproses pembayaran...", Toast.LENGTH_SHORT).show();
        processOrder();
    }

    // ─── Login Redirect ──────────────────────────────────────────────────────────

    /**
     * Redirect ke LoginActivity. Setelah login berhasil, lanjutkan checkout.
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                // Login berhasil → lanjutkan checkout
                loadUserAndCheckout();
            } else {
                // User batal login → kembali ke cart
                Toast.makeText(this, "Login diperlukan untuk checkout.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    /**
     * Load profil user dari SQLite, lalu load data cart.
     */
    private void loadUserAndCheckout() {
        int userId = sessionManager.getUserId();
        executor.execute(() -> {
            currentUser = userDao.getUserById(userId);
            cartItems   = cartDao.getCartItems();
            totalPrice  = cartDao.getCartTotal();

            runOnUiThread(() -> {
                if (cartItems == null || cartItems.isEmpty()) {
                    Toast.makeText(this, "Keranjang kosong.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                displayUserInfo();
                displayCheckoutSummary();
                prefillSavedData();
            });
        });
    }

    /**
     * Tampilkan nama user di header checkout.
     */
    private void displayUserInfo() {
        if (tvUserName != null && currentUser != null) {
            tvUserName.setText("Halo, " + currentUser.getName() + " 👋");
        }
    }

    private void displayCheckoutSummary() {
        if (tvCheckoutTotal != null) {
            tvCheckoutTotal.setText(
                    getString(R.string.total_payment) +
                    LanguageManager.formatPrice(this, totalPrice));
        }
    }

    /**
     * Pre-fill alamat dan payment method dari data yang sudah disimpan sebelumnya.
     */
    private void prefillSavedData() {
        if (currentUser == null) return;

        // Isi alamat tersimpan
        if (etShippingAddress != null && !TextUtils.isEmpty(currentUser.getAddress())) {
            etShippingAddress.setText(currentUser.getAddress());
        }

        // Isi nomor hp tersimpan
        if (etPhoneNumber != null && !TextUtils.isEmpty(currentUser.getPhoneNumber())) {
            etPhoneNumber.setText(currentUser.getPhoneNumber());
        }

        // Pilih payment method default
        if (rgPaymentMethod != null && !TextUtils.isEmpty(currentUser.getDefaultPayment())) {
            String savedPayment = currentUser.getDefaultPayment();
            for (int i = 0; i < rgPaymentMethod.getChildCount(); i++) {
                View child = rgPaymentMethod.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton rb = (RadioButton) child;
                    if (savedPayment.equals(rb.getText().toString())) {
                        rb.setChecked(true);
                        break;
                    }
                }
            }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    private String getSelectedPaymentMethod() {
        if (rgPaymentMethod == null) return null;
        int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
        if (selectedId == -1) return null;
        RadioButton selected = rgPaymentMethod.findViewById(selectedId);
        return selected != null ? selected.getText().toString() : null;
    }

    private boolean validateForm() {
        // Validasi alamat
        String address = etShippingAddress != null
                ? etShippingAddress.getText().toString().trim() : "";
        if (TextUtils.isEmpty(address)) {
            if (etShippingAddress != null) etShippingAddress.setError("Alamat pengiriman wajib diisi");
            return false;
        }

        // Validasi nomor hp
        String phone = etPhoneNumber != null
                ? etPhoneNumber.getText().toString().trim() : "";
        if (TextUtils.isEmpty(phone)) {
            if (etPhoneNumber != null) etPhoneNumber.setError("Nomor HP wajib diisi");
            return false;
        }

        // Validasi payment method
        if (getSelectedPaymentMethod() == null) {
            Toast.makeText(this, "Pilih metode pembayaran terlebih dahulu.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void placeOrder() {
        if (!validateForm()) return;

        String paymentMethod = getSelectedPaymentMethod();

        // Jika NFC Payment → tampilkan dialog tunggu, proses order saat NFC tap
        if ("NFC Payment".equals(paymentMethod)) {
            if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
                showNfcGuide();
                return;
            }
            showNfcWaitingDialog();
            return;
        }

        processOrder();
    }

    /**
     * Proses order: simpan ke SQLite, update profil user, hapus cart.
     */
    private void processOrder() {
        String paymentMethod = getSelectedPaymentMethod();
        String address = etShippingAddress != null
                ? etShippingAddress.getText().toString().trim() : "";
        String phone = etPhoneNumber != null
                ? etPhoneNumber.getText().toString().trim() : "";
        executor.execute(() -> {
            // 1. Buat order dengan userId dan alamat
            long orderId = orderDao.createOrder(
                    sessionManager.getUserId(),
                    totalPrice,
                    paymentMethod,
                    Order.STATUS_WAITING,
                    address
            );

            if (orderId != -1) {
                // 2. Simpan detail item
                orderDao.insertOrderDetail(orderId, cartItems);

                // 3. Simpan alamat, phone & payment ke profil user agar pre-fill berikutnya
                userDao.updateProfile(sessionManager.getUserId(), address, phone, paymentMethod);

                // 4. Kosongkan cart
                cartDao.clearCart();

                runOnUiThread(this::showSuccessMessage);
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Gagal membuat order. Coba lagi.",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showSuccessMessage() {
        if (layoutCheckout != null) layoutCheckout.setVisibility(View.GONE);
        if (layoutSuccess != null) {
            layoutSuccess.setVisibility(View.VISIBLE);
            Button btnBackHome = layoutSuccess.findViewById(R.id.btn_back_to_home);
            if (btnBackHome != null) {
                btnBackHome.setOnClickListener(v -> finish());
            }
        }
        Toast.makeText(this, "✅ Pesanan berhasil dibuat!", Toast.LENGTH_LONG).show();
    }
}
