package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.furniture.R;
import com.example.furniture.api.ApiService;
import com.example.furniture.api.RetrofitClient;
import com.example.furniture.database.CartDao;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.FavoriteDao;
import com.example.furniture.database.ProductDao;
import com.example.furniture.model.Product;
import com.example.furniture.model.ProductDetailResponse;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.NetworkUtils;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DetailActivity — menampilkan detail satu produk.
 * Menerima product_id via Intent, mengambil detail dari API atau cache SQLite.
 */
public class DetailActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private ImageView imgProduct;
    private TextView tvName;
    private TextView tvPrice;
    private TextView tvRating;
    private TextView tvDescription;
    private TextView tvCategory;
    private Button btnAddToCart;
    private ImageButton btnFavorite;
    private ProgressBar progressBar;
    private TextView tvError;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private String productId;
    private Product currentProduct;
    private FavoriteDao favoriteDao;
    private CartDao cartDao;
    private ProductDao productDao;
    private boolean isFavorite = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Tombol back di toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initView();
        getIntentData();
        loadProductDetail();
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
        imgProduct   = findViewById(R.id.img_detail_product);
        tvName       = findViewById(R.id.tv_detail_name);
        tvPrice      = findViewById(R.id.tv_detail_price);
        tvRating     = findViewById(R.id.tv_detail_rating);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvCategory   = findViewById(R.id.tv_detail_category);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
        btnFavorite  = findViewById(R.id.btn_detail_favorite);
        progressBar  = findViewById(R.id.progress_detail);
        tvError      = findViewById(R.id.tv_detail_error);

        // Setup DAO
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        favoriteDao = new FavoriteDao(dbHelper);
        cartDao     = new CartDao(dbHelper);
        productDao  = new ProductDao(dbHelper);

        // Button listeners
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> addToCart());
        }
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }
    }

    /**
     * Ambil product_id dari Intent.
     */
    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getStringExtra(Constants.EXTRA_PRODUCT_ID);
        }

        if (productId == null || productId.isEmpty()) {
            showError("ID produk tidak valid.");
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    /**
     * Ambil detail produk dari API atau dari cache SQLite.
     */
    private void loadProductDetail() {
        if (productId == null) return;

        if (NetworkUtils.isInternetAvailable(this)) {
            loadFromApi();
        } else {
            loadFromCache();
        }
    }

    private void loadFromApi() {
        showLoading();
        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductDetailResponse> call = apiService.getProductDetail(productId);

        call.enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductDetailResponse> call,
                                   @NonNull Response<ProductDetailResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getPayload() != null) {

                    currentProduct = response.body().getPayload().getProduct();
                    if (currentProduct != null) {
                        showProductDetail(currentProduct);
                        updateFavoriteStatus();
                    } else {
                        loadFromCache();
                    }
                } else {
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductDetailResponse> call, @NonNull Throwable t) {
                hideLoading();
                loadFromCache();
            }
        });
    }

    /**
     * Fallback: ambil dari SQLite products_cache.
     */
    private void loadFromCache() {
        showLoading();
        executor.execute(() -> {
            Product cached = productDao.getProductById(productId);
            runOnUiThread(() -> {
                hideLoading();
                if (cached != null) {
                    currentProduct = cached;
                    showProductDetail(cached);
                    updateFavoriteStatus();
                } else {
                    showError("Produk tidak ditemukan. Coba saat online.");
                }
            });
        });
    }

    // ─── Display ─────────────────────────────────────────────────────────────────

    private void showProductDetail(Product product) {
        if (tvName != null) tvName.setText(product.getName());
        if (tvPrice != null) tvPrice.setText(formatPrice(product.getPrice()));
        if (tvRating != null) tvRating.setText("⭐ " + product.getRating()
                + " (" + product.getReviewCount() + " reviews)");
        if (tvDescription != null) tvDescription.setText(product.getDescription());
        if (tvCategory != null) tvCategory.setText(product.getCategory());

        if (imgProduct != null) {
            Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgProduct);
        }

        // Tampilkan tombol aksi
        if (btnAddToCart != null) btnAddToCart.setVisibility(View.VISIBLE);
        if (btnFavorite != null) btnFavorite.setVisibility(View.VISIBLE);
    }

    /**
     * Update ikon favorit berdasarkan status di SQLite.
     */
    private void updateFavoriteStatus() {
        executor.execute(() -> {
            isFavorite = favoriteDao.isFavorite(productId);
            runOnUiThread(() -> {
                if (btnFavorite != null) {
                    btnFavorite.setImageResource(isFavorite
                            ? R.drawable.ic_favorite_filled
                            : R.drawable.ic_favorite_border);
                }
            });
        });
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    private void addToCart() {
        if (currentProduct == null) return;

        executor.execute(() -> {
            cartDao.addToCart(currentProduct);
            runOnUiThread(() ->
                    Toast.makeText(this, currentProduct.getName() + " ditambahkan ke cart 🛒",
                            Toast.LENGTH_SHORT).show());
        });
    }

    private void toggleFavorite() {
        if (currentProduct == null) return;

        executor.execute(() -> {
            if (isFavorite) {
                favoriteDao.removeFavorite(productId);
                isFavorite = false;
            } else {
                favoriteDao.addFavorite(currentProduct);
                isFavorite = true;
            }

            runOnUiThread(() -> {
                if (btnFavorite != null) {
                    btnFavorite.setImageResource(isFavorite
                            ? R.drawable.ic_favorite_filled
                            : R.drawable.ic_favorite_border);
                }
                Toast.makeText(this,
                        isFavorite ? "Ditambahkan ke favorit ❤" : "Dihapus dari favorit",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        if (tvError != null) {
            tvError.setText(msg);
            tvError.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private String formatPrice(double price) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        return nf.format(price);
    }
}
