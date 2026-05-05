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
import com.example.furniture.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView untuk daftar produk.
 * Digunakan di HomeFragment dan CategoryFragment.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    // ─── Interface ───────────────────────────────────────────────────────────────

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product);
    }

    // ─── Fields ──────────────────────────────────────────────────────────────────

    private final Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public ProductAdapter(Context context) {
        this.context = context;
        this.productList = new ArrayList<>();
    }

    public ProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.listener = listener;
    }

    // ─── Public Methods ──────────────────────────────────────────────────────────

    public void setListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    /**
     * Mengganti seluruh data produk dan refresh tampilan.
     */
    public void setProducts(List<Product> products) {
        this.productList = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Menambah produk ke list yang sudah ada (untuk pagination).
     */
    public void addProducts(List<Product> products) {
        if (products != null) {
            int startPos = productList.size();
            productList.addAll(products);
            notifyItemRangeInserted(startPos, products.size());
        }
    }

    public void clearProducts() {
        productList.clear();
        notifyDataSetChanged();
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────────

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName;
        private final TextView tvPrice;
        private final TextView tvRating;
        private final ImageButton btnFavorite;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct  = itemView.findViewById(R.id.img_product);
            tvName      = itemView.findViewById(R.id.tv_product_name);
            tvPrice     = itemView.findViewById(R.id.tv_product_price);
            tvRating    = itemView.findViewById(R.id.tv_product_rating);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }

        void bind(Product product) {
            tvName.setText(product.getName());
            tvPrice.setText(formatPrice(product.getPrice()));
            tvRating.setText(String.valueOf(product.getRating()));

            // Load gambar dengan Glide
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(imgProduct);

            // Klik item → buka DetailActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });

            // Klik tombol favorit
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    if (listener != null) listener.onFavoriteClick(product);
                });
            }
        }

        private String formatPrice(double price) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
            return nf.format(price);
        }
    }
}
