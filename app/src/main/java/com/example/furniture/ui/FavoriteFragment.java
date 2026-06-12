package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.FavoriteAdapter;
import com.example.furniture.database.DatabaseHelper;
import com.example.furniture.database.FavoriteDao;
import com.example.furniture.model.FavoriteItem;
import com.example.furniture.utils.Constants;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FavoriteFragment — menampilkan daftar produk favorit/wishlist dari SQLite.
 */
public class FavoriteFragment extends Fragment implements FavoriteAdapter.OnFavoriteClickListener {

    // ─── Views ───────────────────────────────────────────────────────────────────

    private RecyclerView recyclerView;
    private View layoutEmpty;
    private View layoutBottom;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private FavoriteAdapter favoriteAdapter;
    private FavoriteDao favoriteDao;
    private com.example.furniture.database.CartDao cartDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload saat kembali dari DetailActivity
        loadFavorites();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.rv_favorites);
        layoutEmpty  = view.findViewById(R.id.layout_empty_favorite);
        layoutBottom = view.findViewById(R.id.layout_bottom);

        favoriteDao = new FavoriteDao(DatabaseHelper.getInstance(requireContext()));
        cartDao = new com.example.furniture.database.CartDao(DatabaseHelper.getInstance(requireContext()));

        favoriteAdapter = new FavoriteAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(favoriteAdapter);

        View btnAddAll = view.findViewById(R.id.btn_add_all);
        if (btnAddAll != null) {
            btnAddAll.setOnClickListener(v -> addAllToCart());
        }
    }

    // ─── Load Data ───────────────────────────────────────────────────────────────

    private void loadFavorites() {
        executor.execute(() -> {
            List<FavoriteItem> favorites = favoriteDao.getAllFavorites();
            requireActivity().runOnUiThread(() -> {
                if (favorites != null && !favorites.isEmpty()) {
                    favoriteAdapter.setFavorites(favorites);
                    showContent();
                } else {
                    showEmptyState();
                }
            });
        });
    }

    private void removeFavorite(FavoriteItem item) {
        executor.execute(() -> {
            favoriteDao.removeFavorite(item.getProductId());
            requireActivity().runOnUiThread(() -> {
                favoriteAdapter.removeItem(item);
                if (favoriteAdapter.isEmpty()) {
                    showEmptyState();
                }
            });
        });
    }

    private void addAllToCart() {
        executor.execute(() -> {
            List<FavoriteItem> favorites = favoriteDao.getAllFavorites();
            if (favorites != null && !favorites.isEmpty()) {
                for (FavoriteItem item : favorites) {
                    com.example.furniture.model.Product product = new com.example.furniture.model.Product();
                    product.setProductId(item.getProductId());
                    product.setName(item.getName());
                    product.setPrice(item.getPrice());
                    product.setImageUrl(item.getImageUrl());
                    
                    cartDao.addToCart(product);
                }
                requireActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(requireContext(), "All items added to cart!", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showContent() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (layoutBottom != null) layoutBottom.setVisibility(View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (layoutBottom != null) layoutBottom.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
    }

    // ─── FavoriteAdapter.OnFavoriteClickListener ─────────────────────────────────

    @Override
    public void onProductClick(FavoriteItem item) {
        // Buka DetailActivity
        Intent intent = new Intent(requireContext(), DetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.getProductId());
        startActivity(intent);
    }

    @Override
    public void onRemoveClick(FavoriteItem item) {
        removeFavorite(item);
    }

    @Override
    public void onAddToCartClick(FavoriteItem item) {
        executor.execute(() -> {
            com.example.furniture.model.Product product = new com.example.furniture.model.Product();
            product.setProductId(item.getProductId());
            product.setName(item.getName());
            product.setPrice(item.getPrice());
            product.setImageUrl(item.getImageUrl());
            
            cartDao.addToCart(product);
            requireActivity().runOnUiThread(() -> {
                android.widget.Toast.makeText(requireContext(), "Item added to cart", android.widget.Toast.LENGTH_SHORT).show();
            });
        });
    }
}
