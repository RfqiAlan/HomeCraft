package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.furniture.utils.SessionManager;

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

    private static final int REQUEST_LOGIN = 200;

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView rvCheckoutItems;
    private TextView tvCheckoutTotal;
    private TextView tvUserName;
    private EditText etShippingAddress;
    private RadioGroup rgPaymentMethod;
    private Button btnPlaceOrder;
    private View layoutSuccess;
    private View layoutCheckout;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private CartDao cartDao;
    private OrderDao orderDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private List<CartItem> cartItems;
    private double totalPrice = 0.0;
    private User currentUser;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        // ─── Cek login sebelum lanjut ──────────────────────────────────────────
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        } else {
            loadUserAndCheckout();
        }
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
        rvCheckoutItems  = findViewById(R.id.rv_checkout_items);
        tvCheckoutTotal  = findViewById(R.id.tv_checkout_total);
        tvUserName       = findViewById(R.id.tv_checkout_user_name);
        etShippingAddress = findViewById(R.id.et_shipping_address);
        rgPaymentMethod  = findViewById(R.id.rg_payment_method);
        btnPlaceOrder    = findViewById(R.id.btn_place_order);
        layoutSuccess    = findViewById(R.id.layout_order_success);
        layoutCheckout   = findViewById(R.id.layout_checkout_content);

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

        setupPaymentMethods();
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
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        if (tvCheckoutTotal != null) {
            tvCheckoutTotal.setText("Total Pembayaran: " + nf.format(totalPrice));
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

        // Validasi payment method
        if (getSelectedPaymentMethod() == null) {
            Toast.makeText(this, "Pilih metode pembayaran terlebih dahulu.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Proses order: simpan ke SQLite, update profil user, hapus cart.
     */
    private void placeOrder() {
        if (!validateForm()) return;

        String paymentMethod = getSelectedPaymentMethod();
        String address = etShippingAddress != null
                ? etShippingAddress.getText().toString().trim() : "";

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

                // 3. Simpan alamat & payment ke profil user agar pre-fill berikutnya
                userDao.updateProfile(sessionManager.getUserId(), address, paymentMethod);

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
