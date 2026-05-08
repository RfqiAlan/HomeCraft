package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.furniture.model.Category;
import com.example.furniture.model.CategoryResponse;
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
public class HomeFragment extends Fragment implements ProductAdapter.OnProductClickListener, CategoryAdapter.OnCategoryClickListener {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView recyclerView;
    private RecyclerView rvCategories;
    private ProgressBar progressBar;
    private TextView tvError;
    private Button btnRefresh;
    private SwipeRefreshLayout swipeRefresh;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private ProductDao productDao;
    private String currentCategoryId = Constants.FURNITURE_CATEGORY_ID;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        loadCategories();
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
        rvCategories  = view.findViewById(R.id.rv_categories);
        progressBar   = view.findViewById(R.id.progress_bar);
        tvError       = view.findViewById(R.id.tv_error);
        btnRefresh    = view.findViewById(R.id.btn_refresh);
        swipeRefresh  = view.findViewById(R.id.swipe_refresh);

        // Setup Categories RecyclerView
        if (rvCategories != null) {
            categoryAdapter = new CategoryAdapter(requireContext(), this);
            rvCategories.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            rvCategories.setAdapter(categoryAdapter);
        }

        // Setup RecyclerView dengan GridLayout 2 kolom
        productAdapter = new ProductAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(productAdapter);

        // Setup DAO
        productDao = new ProductDao(DatabaseHelper.getInstance(requireContext()));

        // Tombol refresh manual
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                loadCategories();
                loadProducts();
            });
        }

        // Swipe to refresh
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadCategories();
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

    private void loadCategories() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) return;

        ApiService apiService = RetrofitClient.getApiService();
        Call<CategoryResponse> call = apiService.getCategories();
        call.enqueue(new Callback<CategoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryResponse> call, @NonNull Response<CategoryResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getPayload() != null) {
                    List<Category> allCategories = response.body().getPayload().getCategories();
                    List<Category> furnitureSubcategories = filterFurnitureCategories(allCategories);
                    
                    if (!furnitureSubcategories.isEmpty()) {
                        // Tambahkan pseudo-kategori "All" di awal
                        Category allCategory = new Category();
                        allCategory.setId(Constants.FURNITURE_CATEGORY_ID);
                        allCategory.setName("All");
                        furnitureSubcategories.add(0, allCategory);
                        
                        if (categoryAdapter != null) {
                            categoryAdapter.setCategories(furnitureSubcategories);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoryResponse> call, @NonNull Throwable t) {
                // Ignore failure for categories
            }
        });
    }

    private List<Category> filterFurnitureCategories(List<Category> categories) {
        List<Category> result = new ArrayList<>();
        if (categories == null) return result;
        for (Category cat : categories) {
            if (Constants.FURNITURE_CATEGORY_ID.equals(cat.getId()) ||
                    Constants.FURNITURE_CATEGORY_NAME.equalsIgnoreCase(cat.getName())) {
                if (cat.getSubCategories() != null) {
                    result.addAll(cat.getSubCategories());
                }
                return result;
            }
            if (cat.getSubCategories() != null) {
                List<Category> found = filterFurnitureCategories(cat.getSubCategories());
                if (!found.isEmpty()) return found;
            }
        }
        return result;
    }

    /**
     * Ambil produk dari API Retrofit.
     */
    private void loadProductsFromApi() {
        showLoading();

        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductResponse> call = apiService.getProducts(
                currentCategoryId,
                Constants.DEFAULT_LIMIT,
                Constants.DEFAULT_OFFSET,
                Constants.DEFAULT_SORT_ID);

        call.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call,
                                   @NonNull Response<ProductResponse> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ProductResponse body = response.body();
                    if (body.getPayload() != null &&
                            body.getPayload().getProducts() != null &&
                            !body.getPayload().getProducts().isEmpty()) {

                        List<Product> products = body.getPayload().getProducts();
                        productAdapter.setProducts(products);
                        showContent();

                        // Simpan ke SQLite di background thread hanya jika ini "All" / kategori utama
                        if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID)) {
                            saveProductsToLocal(products);
                        }
                    } else {
                        if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID)) {
                            loadProductsFromLocal();
                        } else {
                            showError("Tidak ada produk di kategori ini.");
                        }
                    }
                } else {
                    if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID)) {
                        loadProductsFromLocal();
                    } else {
                        showError("Gagal memuat produk. Coba lagi.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                hideLoading();
                if (currentCategoryId.equals(Constants.FURNITURE_CATEGORY_ID)) {
                    loadProductsFromLocal();
                } else {
                    showError("Gagal terhubung ke server.");
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

    @Override
    public void onCategoryClick(Category category) {
        currentCategoryId = category.getId();
        loadProducts(); // Load products based on the new category ID
    }
}
