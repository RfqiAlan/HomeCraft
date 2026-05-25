package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.CartAdapter;
import com.example.furniture.database.CartDao;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.model.CartItem;
import com.example.furniture.utils.LanguageManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CartFragment — menampilkan keranjang belanja.
 * Update quantity, hapus item, hitung total, dan navigasi ke CheckoutActivity.
 */
public class CartFragment extends Fragment implements CartAdapter.OnCartActionListener {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvTotal;
    private Button btnCheckout;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private CartAdapter cartAdapter;
    private CartDao cartDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCartItems();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.rv_cart);
        tvEmpty      = view.findViewById(R.id.tv_cart_empty);
        tvTotal      = view.findViewById(R.id.tv_cart_total);
        btnCheckout  = view.findViewById(R.id.btn_checkout);

        cartDao = new CartDao(DatabaseHelper.getInstance(requireContext()));

        cartAdapter = new CartAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(cartAdapter);

        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> goToCheckout());
        }
        
        View btnShareCart = view.findViewById(R.id.btn_share_cart);
        if (btnShareCart != null) {
            btnShareCart.setOnClickListener(v -> shareCart());
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    private void loadCartItems() {
        executor.execute(() -> {
            List<CartItem> items = cartDao.getCartItems();
            double total = cartDao.getCartTotal();

            requireActivity().runOnUiThread(() -> {
                if (items != null && !items.isEmpty()) {
                    cartAdapter.setCartItems(items);
                    calculateTotal(total);
                    showContent();
                } else {
                    showEmptyState();
                }
            });
        });
    }

    // ─── Cart Actions ────────────────────────────────────────────────────────────

    private void calculateTotal(double total) {
        if (tvTotal != null) {
            tvTotal.setText(LanguageManager.formatPrice(requireContext(), total));
        }
    }

    private void goToCheckout() {
        if (cartAdapter.isEmpty()) {
            Toast.makeText(requireContext(), "Keranjang masih kosong.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), CheckoutActivity.class);
        startActivity(intent);
    }

    private void shareCart() {
        if (cartAdapter.isEmpty()) {
            Toast.makeText(requireContext(), "Keranjang belanja Anda kosong.", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            List<CartItem> items = cartDao.getCartItems();
            int totalItems = 0;
            for (CartItem item : items) {
                totalItems += item.getQuantity();
            }
            
            final int count = totalItems;
            
            requireActivity().runOnUiThread(() -> {
                String shareUrl = "http://homecraft.com/cart";
                String shareText = "Saya punya " + count + " item menarik di keranjang HomeCraft saya! Yuk, intip keranjangnya:\n" + shareUrl;

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Keranjang Belanja HomeCraft Saya");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                startActivity(Intent.createChooser(shareIntent, "Bagikan keranjang via..."));
            });
        });
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showContent() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
        if (btnCheckout != null) btnCheckout.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (tvTotal != null) tvTotal.setVisibility(View.GONE);
        if (btnCheckout != null) btnCheckout.setVisibility(View.GONE);
        if (tvEmpty != null) {
            tvEmpty.setText("Keranjang belanja kosong.\nTambahkan produk dari halaman detail.");
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    // ─── CartAdapter.OnCartActionListener ────────────────────────────────────────

    @Override
    public void onIncrease(CartItem item) {
        executor.execute(() -> {
            cartDao.increaseQuantity(item.getProductId());
            item.increaseQuantity();
            double newTotal = cartDao.getCartTotal();
            requireActivity().runOnUiThread(() -> {
                cartAdapter.updateItem(item);
                calculateTotal(newTotal);
            });
        });
    }

    @Override
    public void onDecrease(CartItem item) {
        if (item.getQuantity() <= 1) {
            // Quantity sudah 1, tampilkan konfirmasi hapus
            onRemove(item);
            return;
        }
        executor.execute(() -> {
            cartDao.decreaseQuantity(item.getProductId());
            item.decreaseQuantity();
            double newTotal = cartDao.getCartTotal();
            requireActivity().runOnUiThread(() -> {
                cartAdapter.updateItem(item);
                calculateTotal(newTotal);
            });
        });
    }

    @Override
    public void onRemove(CartItem item) {
        executor.execute(() -> {
            cartDao.removeFromCart(item.getProductId());
            requireActivity().runOnUiThread(() -> {
                cartAdapter.removeItem(item);
                if (cartAdapter.isEmpty()) {
                    showEmptyState();
                } else {
                    // Recalculate total setelah hapus
                    executor.execute(() -> {
                        double newTotal = cartDao.getCartTotal();
                        requireActivity().runOnUiThread(() -> calculateTotal(newTotal));
                    });
                }
            });
        });
    }
}
