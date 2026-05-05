package com.example.furniture.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.model.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter RecyclerView untuk daftar kategori produk.
 * Digunakan di CategoryFragment.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    // ─── Interface ───────────────────────────────────────────────────────────────

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    // ─── Fields ──────────────────────────────────────────────────────────────────

    private final Context context;
    private List<Category> categoryList;
    private OnCategoryClickListener listener;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = new ArrayList<>();
        this.listener = listener;
    }

    // ─── Public Methods ──────────────────────────────────────────────────────────

    public void setCategories(List<Category> categories) {
        this.categoryList = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────────

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCategoryName;
        private final ImageView imgCategoryIcon;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName  = itemView.findViewById(R.id.tv_category_name);
            imgCategoryIcon = itemView.findViewById(R.id.img_category_icon);
        }

        void bind(Category category) {
            tvCategoryName.setText(category.getName());

            // Klik item kategori
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(category);
            });
        }
    }
}
