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
    private TextView tvEmpty;

    // ─── Data ─────────────────────────────────────────────────────────────────────

    private FavoriteAdapter favoriteAdapter;
    private FavoriteDao favoriteDao;
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
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }

    // ─── Init ────────────────────────────────────────────────────────────────────

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.rv_favorites);
        tvEmpty      = view.findViewById(R.id.tv_favorite_empty);

        favoriteDao = new FavoriteDao(DatabaseHelper.getInstance(requireContext()));

        favoriteAdapter = new FavoriteAdapter(requireContext(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(favoriteAdapter);
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

    // ─── UI State ────────────────────────────────────────────────────────────────

    private void showContent() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (tvEmpty != null) {
            tvEmpty.setText("Belum ada produk favorit.\nTambahkan dari halaman detail produk.");
            tvEmpty.setVisibility(View.VISIBLE);
        }
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
}
