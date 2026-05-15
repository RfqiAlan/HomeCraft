package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.furniture.R;
import com.example.furniture.adapter.CategoryAdapter;
import com.example.furniture.adapter.ProductAdapter;
import com.example.furniture.api.ApiService;
import com.example.furniture.api.RetrofitClient;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.ProductDao;
import com.example.furniture.model.Product;
import com.example.furniture.model.ProductResponse;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.NetworkUtils;
import com.example.furniture.utils.SweetDialog;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment — menampilkan daftar produk furniture.
 * Mengambil data dari API, menyimpan ke SQLite, dan fallback ke cache saat offline.
 */
public class HomeFragment extends Fragment implements ProductAdapter.OnProductClickListener {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageView imgFilterCategory;
    private ProgressBar progressBar;
    private TextView tvError;
    private Button btnRefresh;
    private SwipeRefreshLayout swipeRefresh;
    private android.view.View layoutEmptySearch;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private ProductAdapter productAdapter;
    private ProductDao productDao;
    private String currentCategoryId = Constants.FURNITURE_CATEGORY_ID;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    
    private String currentKeyword = Constants.DEFAULT_HOME_KEYWORD;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        loadProducts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        recyclerView  = view.findViewById(R.id.rv_products);
        etSearch      = view.findViewById(R.id.et_search);
        imgFilterCategory = view.findViewById(R.id.img_filter_category);
        progressBar   = view.findViewById(R.id.progress_bar);
        tvError       = view.findViewById(R.id.tv_error);
        btnRefresh    = view.findViewById(R.id.btn_refresh);
        swipeRefresh  = view.findViewById(R.id.swipe_refresh);
        layoutEmptySearch = view.findViewById(R.id.layout_empty_search);

        // Setup Search and Filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> {
                        String query = s.toString().trim();
                        // Jika kosong, kembali ke keyword default rumahan
                        // Jika ada query, inject keyword rumahan di belakang agar tidak keluar produk fashion
                        if (query.isEmpty()) {
                            currentKeyword = Constants.DEFAULT_HOME_KEYWORD;
                        } else {
                            currentKeyword = query + " home decor furniture";
                        }
                        currentOffset = 0;
                        isLastPage = false;
                        loadProductsFromApi();
                    };
                    searchHandler.postDelayed(searchRunnable, 500);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        if (imgFilterCategory != null) {
            imgFilterCategory.setOnClickListener(v -> {
                String[] options = {
                        "Harga: Terendah - Tertinggi",
                        "Harga: Tertinggi - Terendah",
                        "Rating: Tertinggi - Terendah",
                        "Rating: Terendah - Tertinggi"
                };

                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Urutkan Produk")
                        .setItems(options, (dialog, which) -> {
                            if (productAdapter != null) {
                                switch (which) {
                                    case 0:
                                        productAdapter.sortProducts(ProductAdapter.SORT_PRICE_ASC);
                                        break;
                                    case 1:
                                        productAdapter.sortProducts(ProductAdapter.SORT_PRICE_DESC);
                                        break;
                                    case 2:
                                        productAdapter.sortProducts(ProductAdapter.SORT_RATING_DESC);
                                        break;
                                    case 3:
                                        productAdapter.sortProducts(ProductAdapter.SORT_RATING_ASC);
                                        break;
                                }
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        // Setup RecyclerView dengan GridLayout 2 kolom
        productAdapter = new ProductAdapter(requireContext(), this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(productAdapter);
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoading && !isLastPage) {
                    if (gridLayoutManager.findLastCompletelyVisibleItemPosition() >= productAdapter.getItemCount() - 4) {
                        currentOffset += Constants.DEFAULT_LIMIT;
                        loadProductsFromApi();
                    }
                }
            }
        });

        // Setup DAO
        productDao = new ProductDao(DatabaseHelper.getInstance(requireContext()));

        // Tombol refresh manual
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                currentOffset = 0;
                isLastPage = false;
                loadProducts();
            });
        }

        // Swipe to refresh
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                currentOffset = 0;
                isLastPage = false;
                loadProductsFromApi();
            });
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    /**
     * Entry point: cek koneksi, lalu pilih sumber data.
     */
    private void loadProducts() {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            loadProductsFromApi();
        } else {
            loadProductsFromLocal();
        }
    }



    /**
     * Ambil produk dari API Retrofit.
     */
    private void loadProductsFromApi() {
        if (isLoading) return;
        isLoading = true;

        if (currentOffset == 0) {
            showLoading();
        } else {
            if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(true);
            }
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductResponse> call = apiService.getProducts(
                currentCategoryId,
                currentKeyword,
                Constants.DEFAULT_LIMIT,
                currentOffset,
                Constants.DEFAULT_SORT_ID);

        call.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call,
                                   @NonNull Response<ProductResponse> response) {
                isLoading = false;
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ProductResponse body = response.body();
                    if (body.getPayload() != null &&
                            body.getPayload().getProducts() != null &&
                            !body.getPayload().getProducts().isEmpty()) {

                        List<Product> products = body.getPayload().getProducts();
                        
                        if (currentOffset == 0) {
                            productAdapter.setProducts(products);
                        } else {
                            productAdapter.addProducts(products);
                        }

                        if (products.size() < Constants.DEFAULT_LIMIT) {
                            isLastPage = true;
                        }

                        showContent();
                        if (layoutEmptySearch != null) layoutEmptySearch.setVisibility(android.view.View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(android.view.View.VISIBLE);

                        // Simpan ke SQLite di background thread hanya jika ini "All" / kategori utama dan bukan pencarian
                        if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID) && currentKeyword.equals(Constants.DEFAULT_HOME_KEYWORD) && currentOffset == 0) {
                            saveProductsToLocal(products);
                        }
                    } else {
                        isLastPage = true;
                        if (currentOffset == 0) {
                            if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID) && currentKeyword.equals(Constants.DEFAULT_HOME_KEYWORD)) {
                                loadProductsFromLocal();
                            } else {
                                // Pencarian tidak menemukan hasil - tampilkan empty state
                                if (layoutEmptySearch != null) layoutEmptySearch.setVisibility(android.view.View.VISIBLE);
                                if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
                                hideLoading();
                            }
                        }
                    }
                } else {
                    if (currentOffset == 0) {
                        if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID) && currentKeyword.equals(Constants.DEFAULT_HOME_KEYWORD)) {
                            loadProductsFromLocal();
                        } else {
                            showError("Gagal memuat produk. Coba lagi.");
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                isLoading = false;
                hideLoading();
                if (currentOffset == 0) {
                    if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID) && currentKeyword.equals(Constants.DEFAULT_HOME_KEYWORD)) {
                        loadProductsFromLocal();
                    } else {
                        showError("Gagal terhubung ke server.");
                    }
                }
            }
        });
    }

    /**
     * Ambil produk dari SQLite cache (fallback offline).
     */
    private void loadProductsFromLocal() {
        showLoading();
        executor.execute(() -> {
            List<Product> cached = productDao.getCachedProducts();
            requireActivity().runOnUiThread(() -> {
                hideLoading();
                if (cached != null && !cached.isEmpty()) {
                    productAdapter.setProducts(cached);
                    showContent();
                    Toast.makeText(requireContext(),
                            "Menampilkan data offline", Toast.LENGTH_SHORT).show();
                } else {
                    showError("Tidak ada koneksi internet dan data cache kosong.\nCoba refresh saat online.");
                }
            });
        });
    }

    /**
     * Simpan daftar produk dari API ke SQLite cache (background thread).
     */
    private void saveProductsToLocal(List<Product> products) {
        executor.execute(() -> productDao.insertProducts(products));
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setVisibility(View.GONE);
        if (btnRefresh != null) btnRefresh.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setVisibility(View.GONE);
        if (btnRefresh != null) btnRefresh.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
        if (btnRefresh != null) btnRefresh.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
    }

    // ─── ProductAdapter.OnProductClickListener ───────────────────────────────────

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(requireContext(), DetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Product product) {
        executor.execute(() -> {
            com.example.furniture.database.FavoriteDao favoriteDao =
                    new com.example.furniture.database.FavoriteDao(
                            DatabaseHelper.getInstance(requireContext()));

            boolean isFav = favoriteDao.isFavorite(product.getProductId());
            if (isFav) {
                favoriteDao.removeFavorite(product.getProductId());
            } else {
                favoriteDao.addFavorite(product);
            }
            
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                int dialogType = isFav ? SweetDialog.TYPE_INFO : SweetDialog.TYPE_SUCCESS;
                String msg = isFav ? "Produk dihapus dari favorit" : "Produk ditambahkan ke favorit ❤";
                new SweetDialog(requireContext(), dialogType)
                        .setTitleText("Favorit")
                        .setContentText(msg)
                        .show();
            });
        });
    }

}
