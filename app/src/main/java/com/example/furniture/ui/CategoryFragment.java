package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.CategoryAdapter;
import com.example.furniture.adapter.ProductAdapter;
import com.example.furniture.api.ApiService;
import com.example.furniture.api.RetrofitClient;
import com.example.furniture.model.Category;
import com.example.furniture.model.CategoryResponse;
import com.example.furniture.model.Product;
import com.example.furniture.utils.Constants;
import com.example.furniture.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CategoryFragment — menampilkan kategori furniture dan produk berdasarkan kategori.
 * Mengambil data dari endpoint categories/list, memfilter kategori Furniture.
 */
public class CategoryFragment extends Fragment
        implements CategoryAdapter.OnCategoryClickListener,
                   ProductAdapter.OnProductClickListener {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView rvCategories;
    private RecyclerView rvCategoryProducts;
    private ProgressBar progressBar;
    private TextView tvSectionTitle;
    private TextView tvError;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        loadCategoriesFromApi();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        rvCategories       = view.findViewById(R.id.rv_categories);
        rvCategoryProducts = view.findViewById(R.id.rv_category_products);
        progressBar        = view.findViewById(R.id.progress_bar_category);
        tvSectionTitle     = view.findViewById(R.id.tv_section_title);
        tvError            = view.findViewById(R.id.tv_category_error);

        // Category RecyclerView (horizontal)
        categoryAdapter = new CategoryAdapter(requireContext(), this);
        rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Product RecyclerView (vertical, tampil saat kategori dipilih)
        productAdapter = new ProductAdapter(requireContext(), this);
        if (rvCategoryProducts != null) {
            rvCategoryProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvCategoryProducts.setAdapter(productAdapter);
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    /**
     * Ambil categories/list dari API, lalu filter kategori Furniture.
     */
    private void loadCategoriesFromApi() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showError("Tidak ada koneksi internet.");
            return;
        }

        showLoading();
        ApiService apiService = RetrofitClient.getApiService();
        Call<CategoryResponse> call = apiService.getCategories();

        call.enqueue(new Callback<CategoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryResponse> call,
                                   @NonNull Response<CategoryResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getPayload() != null) {

                    List<Category> allCategories =
                            response.body().getPayload().getCategories();
                    List<Category> furnitureSubcategories =
                            filterFurnitureCategories(allCategories);

                    if (!furnitureSubcategories.isEmpty()) {
                        categoryAdapter.setCategories(furnitureSubcategories);
                    } else {
                        showError("Kategori furniture tidak ditemukan.");
                    }
                } else {
                    showError("Gagal memuat kategori. Coba lagi.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoryResponse> call, @NonNull Throwable t) {
                hideLoading();
                showError("Gagal terhubung ke server.");
            }
        });
    }

    /**
     * Filter kategori Furniture dari seluruh tree kategori API.
     * Alur: cari "Furniture" (ID: 1350311459149) → ambil subkategorinya.
     */
    private List<Category> filterFurnitureCategories(List<Category> categories) {
        List<Category> result = new ArrayList<>();
        if (categories == null) return result;

        for (Category cat : categories) {
            // Cari kategori Furniture berdasarkan ID atau nama
            if (Constants.FURNITURE_CATEGORY_ID.equals(cat.getId()) ||
                    Constants.FURNITURE_CATEGORY_NAME.equalsIgnoreCase(cat.getName())) {

                // Tambahkan kategori Furniture itu sendiri
                result.add(cat);

                // Tambahkan subkategorinya jika ada
                if (cat.getSubCategories() != null) {
                    result.addAll(cat.getSubCategories());
                }
                return result;
            }

            // Rekursif cari di subcategory
            if (cat.getSubCategories() != null) {
                List<Category> found = filterFurnitureCategories(cat.getSubCategories());
                if (!found.isEmpty()) return found;
            }
        }
        return result;
    }

    /**
     * Ambil produk berdasarkan kategori yang dipilih.
     */
    private void loadProductsByCategory(Category category) {
        if (tvSectionTitle != null) {
            tvSectionTitle.setText(category.getName());
        }

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Tidak ada koneksi internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        ApiService apiService = RetrofitClient.getApiService();
        Call<com.example.furniture.model.ProductResponse> call =
                apiService.getProducts(category.getId(), 1);

        call.enqueue(new Callback<com.example.furniture.model.ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.furniture.model.ProductResponse> call,
                                   @NonNull Response<com.example.furniture.model.ProductResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getPayload() != null) {

                    List<com.example.furniture.model.Product> products =
                            response.body().getPayload().getProducts();

                    if (products != null && !products.isEmpty()) {
                        productAdapter.setProducts(products);
                        if (rvCategoryProducts != null) {
                            rvCategoryProducts.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                "Tidak ada produk di kategori ini.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.example.furniture.model.ProductResponse> call,
                                  @NonNull Throwable t) {
                hideLoading();
                Toast.makeText(requireContext(), "Gagal memuat produk.", Toast.LENGTH_SHORT).show();
            }
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
    }

    // ─── CategoryAdapter.OnCategoryClickListener ─────────────────────────────────

    @Override
    public void onCategoryClick(Category category) {
        loadProductsByCategory(category);
    }

    // ─── ProductAdapter.OnProductClickListener ───────────────────────────────────

    @Override
    public void onProductClick(com.example.furniture.model.Product product) {
        Intent intent = new Intent(requireContext(), DetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(com.example.furniture.model.Product product) {
        Toast.makeText(requireContext(),
                product.getName() + " ditambahkan ke favorit ❤",
                Toast.LENGTH_SHORT).show();
    }
}
