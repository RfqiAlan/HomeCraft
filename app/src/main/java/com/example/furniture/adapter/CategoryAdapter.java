package com.example.furniture.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private int selectedPosition = -1;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = new ArrayList<>();
        this.listener = listener;
    }

    // ─── Public Methods ──────────────────────────────────────────────────────────

    public void setCategories(List<Category> categories) {
        this.categoryList = categories != null ? categories : new ArrayList<>();
        selectedPosition = -1;
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
        holder.bind(category, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCategoryName;
        private final ImageView imgCategoryIcon;
        private final FrameLayout flCategoryIcon;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName  = itemView.findViewById(R.id.tv_category_name);
            imgCategoryIcon = itemView.findViewById(R.id.img_category_icon);
            flCategoryIcon  = itemView.findViewById(R.id.fl_category_icon);
        }

        void bind(Category category, boolean isSelected) {
            tvCategoryName.setText(category.getName());

            // Resolve dynamic text colors from active theme
            int textColorPrimary = 0;
            int textColorSecondary = 0;
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
                textColorPrimary = typedValue.data;
            } else {
                textColorPrimary = ContextCompat.getColor(context, android.R.color.black);
            }
            if (context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)) {
                textColorSecondary = typedValue.data;
            } else {
                textColorSecondary = ContextCompat.getColor(context, android.R.color.darker_gray);
            }

            // Update background & icon tint berdasarkan state
            if (isSelected) {
                flCategoryIcon.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_category_selected));
                imgCategoryIcon.setColorFilter(
                        ContextCompat.getColor(context, R.color.category_selected_icon));
                tvCategoryName.setTextColor(textColorPrimary);
            } else {
                flCategoryIcon.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_category_unselected));
                imgCategoryIcon.setColorFilter(textColorSecondary);
                tvCategoryName.setTextColor(textColorSecondary);
            }

            // Klik item kategori
            itemView.setOnClickListener(v -> {
                int prevSelected = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(prevSelected);
                notifyItemChanged(selectedPosition);
                if (listener != null) listener.onCategoryClick(category);
            });
        }
    }
}
