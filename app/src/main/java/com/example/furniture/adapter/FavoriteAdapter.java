package com.example.furniture.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.furniture.R;
import com.example.furniture.model.FavoriteItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView untuk daftar produk favorit.
 * Digunakan di FavoriteFragment.
 */
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    // ─── Interface ───────────────────────────────────────────────────────────────

    public interface OnFavoriteClickListener {
        void onProductClick(FavoriteItem item);
        void onRemoveClick(FavoriteItem item);
    }

    // ─── Fields ──────────────────────────────────────────────────────────────────

    private final Context context;
    private List<FavoriteItem> favoriteList;
    private final OnFavoriteClickListener listener;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public FavoriteAdapter(Context context, OnFavoriteClickListener listener) {
        this.context = context;
        this.favoriteList = new ArrayList<>();
        this.listener = listener;
    }

    // ─── Public Methods ──────────────────────────────────────────────────────────

    public void setFavorites(List<FavoriteItem> items) {
        this.favoriteList = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(FavoriteItem item) {
        int index = favoriteList.indexOf(item);
        if (index != -1) {
            favoriteList.remove(index);
            notifyItemRemoved(index);
        }
    }

    public boolean isEmpty() {
        return favoriteList.isEmpty();
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────────

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(favoriteList.get(position));
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class FavoriteViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName;
        private final TextView tvPrice;
        private final ImageButton btnRemove;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_favorite_product);
            tvName     = itemView.findViewById(R.id.tv_favorite_name);
            tvPrice    = itemView.findViewById(R.id.tv_favorite_price);
            btnRemove  = itemView.findViewById(R.id.btn_remove_favorite);
        }

        void bind(FavoriteItem item) {
            tvName.setText(item.getName());
            tvPrice.setText(formatPrice(item.getPrice()));

            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(imgProduct);

            // Klik item → buka DetailActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(item);
            });

            // Klik tombol hapus favorit
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    if (listener != null) listener.onRemoveClick(item);
                });
            }
        }

        private String formatPrice(double price) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
            return nf.format(price);
        }
    }
}
