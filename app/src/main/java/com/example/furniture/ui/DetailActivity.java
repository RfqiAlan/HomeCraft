package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.furniture.R;
import com.example.furniture.adapter.ImageSliderAdapter;
import com.example.furniture.api.ApiService;
import com.example.furniture.api.RetrofitClient;
import com.example.furniture.database.CartDao;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.FavoriteDao;
import com.example.furniture.database.ProductDao;
import com.example.furniture.model.Product;
import com.example.furniture.model.ProductDetailResponse;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.NetworkUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DetailActivity — menampilkan detail satu produk dengan galeri foto (ViewPager2).
 */
public class DetailActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private ViewPager2 vpProductImages;
    private LinearLayout llIndicators;
    private TextView tvName;
    private TextView tvPrice;
    private TextView tvRating;
    private TextView tvDescription;
    private TextView tvCategory;
    private TextView tvQty;
    private Button btnAddToCart;
    private ImageButton btnFavorite;
    private com.google.android.material.button.MaterialButton btnQtyPlus;
    private com.google.android.material.button.MaterialButton btnQtyMinus;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvReviewSummary;
    private com.google.android.material.button.MaterialButton btnSeeReviews;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private String productId;
    private Product currentProduct;
    private FavoriteDao favoriteDao;
    private CartDao cartDao;
    private ProductDao productDao;
    private ImageSliderAdapter sliderAdapter;
    private boolean isFavorite = false;
    private int qty = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        initView();
        getIntentData();
        loadProductDetail();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView() {
        vpProductImages = findViewById(R.id.vp_product_images);
        llIndicators    = findViewById(R.id.ll_indicators);
        tvName          = findViewById(R.id.tv_detail_name);
        tvPrice         = findViewById(R.id.tv_detail_price);
        tvRating        = findViewById(R.id.tv_detail_rating);
        tvDescription   = findViewById(R.id.tv_detail_description);
        tvCategory      = findViewById(R.id.tv_detail_category);
        tvQty           = findViewById(R.id.tv_qty);
        btnAddToCart    = findViewById(R.id.btn_add_to_cart);
        btnFavorite     = findViewById(R.id.btn_detail_favorite);
        btnQtyPlus      = findViewById(R.id.btn_qty_plus);
        btnQtyMinus     = findViewById(R.id.btn_qty_minus);
        progressBar     = findViewById(R.id.progress_detail);
        tvError         = findViewById(R.id.tv_detail_error);
        tvReviewSummary = findViewById(R.id.tv_review_summary);
        btnSeeReviews   = findViewById(R.id.btn_see_reviews);

        // Setup DAO
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        favoriteDao = new FavoriteDao(dbHelper);
        cartDao     = new CartDao(dbHelper);
        productDao  = new ProductDao(dbHelper);

        // Image Slider
        sliderAdapter = new ImageSliderAdapter(new ArrayList<>());
        vpProductImages.setAdapter(sliderAdapter);
        vpProductImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
            }
        });

        // Button listeners
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnAddToCart != null) btnAddToCart.setOnClickListener(v -> addToCart());
        if (btnFavorite  != null) btnFavorite.setOnClickListener(v -> toggleFavorite());
        if (btnSeeReviews != null) btnSeeReviews.setOnClickListener(v -> openReviews());

        if (btnQtyPlus != null) btnQtyPlus.setOnClickListener(v -> {
            qty++;
            tvQty.setText(String.format("%02d", qty));
        });
        if (btnQtyMinus != null) btnQtyMinus.setOnClickListener(v -> {
            if (qty > 1) {
                qty--;
                tvQty.setText(String.format("%02d", qty));
            }
        });
    }

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
                if (response.isSuccessful() && response.body() != null) {
                    currentProduct = response.body().getProduct();
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
        if (tvName     != null) tvName.setText(product.getName());
        if (tvPrice    != null) tvPrice.setText(formatPrice(product.getPrice()));

        String ratingText = product.getRating() + "  (" + product.getReviewCount() + " reviews)";
        if (tvRating   != null) tvRating.setText(ratingText);
        // Update juga summary di section review
        if (tvReviewSummary != null) {
            tvReviewSummary.setText("⭐ " + product.getRating() + "  •  " + product.getReviewCount() + " ulasan");
        }

        if (tvCategory != null) {
            String cat = product.getCategory();
            if (cat != null && !cat.isEmpty()) {
                tvCategory.setText(cat);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }
        }

        // Description — strip HTML tags
        if (tvDescription != null && product.getDescription() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                tvDescription.setText(android.text.Html.fromHtml(
                        product.getDescription(), android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                tvDescription.setText(android.text.Html.fromHtml(product.getDescription()));
            }
        }

        // Image gallery
        List<String> images = product.getImageUrls();
        if (images == null || images.isEmpty()) {
            // Fallback: gunakan thumbnail saja
            images = new ArrayList<>();
            if (product.getImageUrl() != null) images.add(product.getImageUrl());
        }
        sliderAdapter.setImages(images);
        setupIndicators(images.size());
    }

    // ─── Indicators ──────────────────────────────────────────────────────────────

    private void setupIndicators(int count) {
        if (llIndicators == null) return;
        llIndicators.removeAllViews();

        // Buat dot indicator sebanyak jumlah foto (maks 6 tampil)
        int displayCount = Math.min(count, 6);
        for (int i = 0; i < displayCount; i++) {
            View dot = new View(this);
            int dotSize  = (i == 0) ? 24 : 16; // dp
            int dotSizePx = dpToPx(4);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (i == 0) ? dpToPx(24) : dpToPx(16), dpToPx(4));
            params.setMarginEnd(dpToPx(6));
            dot.setLayoutParams(params);
            dot.setBackground(androidx.core.content.ContextCompat.getDrawable(this,
                    i == 0 ? R.drawable.bg_indicator_active : R.drawable.bg_indicator_inactive));
            llIndicators.addView(dot);
        }
    }

    private void updateIndicators(int selectedIndex) {
        if (llIndicators == null) return;
        int count = llIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = llIndicators.getChildAt(i);
            boolean isActive = (i == selectedIndex);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            params.width = dpToPx(isActive ? 24 : 16);
            dot.setLayoutParams(params);
            dot.setBackground(androidx.core.content.ContextCompat.getDrawable(this,
                    isActive ? R.drawable.bg_indicator_active : R.drawable.bg_indicator_inactive));
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ─── Favorite ────────────────────────────────────────────────────────────────

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
                
                if (isFinishing() || isDestroyed()) return;
                
                int dialogType = isFavorite ? com.example.furniture.utils.SweetDialog.TYPE_SUCCESS : com.example.furniture.utils.SweetDialog.TYPE_INFO;
                String msg = isFavorite ? "Produk ditambahkan ke favorit ❤" : "Produk dihapus dari favorit";
                
                new com.example.furniture.utils.SweetDialog(this, dialogType)
                        .setTitleText("Favorit")
                        .setContentText(msg)
                        .show();
            });
        });
    }

    // ─── Reviews ─────────────────────────────────────────────────────────────────

    private void openReviews() {
        if (productId == null) return;
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    // ─── Cart ────────────────────────────────────────────────────────────────────

    private void addToCart() {
        if (currentProduct == null) return;
        executor.execute(() -> {
            // Tambahkan sesuai qty yang dipilih
            for (int i = 0; i < qty; i++) {
                cartDao.addToCart(currentProduct);
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                new com.example.furniture.utils.SweetDialog(this, com.example.furniture.utils.SweetDialog.TYPE_SUCCESS)
                        .setTitleText("Berhasil")
                        .setContentText(currentProduct.getName() + " (x" + qty + ") ditambahkan ke cart 🛒")
                        .show();
            });
        });
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvError     != null) tvError.setVisibility(View.GONE);
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
        return LanguageManager.formatPrice(this, price);
    }
}
