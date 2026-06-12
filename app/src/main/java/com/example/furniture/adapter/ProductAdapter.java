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
import com.example.furniture.utils.LanguageManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView untuk daftar produk.
 * Digunakan di HomeFragment dan CategoryFragment.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    // ─── Interface ───────────────────────────────────────────────────────────────

    public interface OnProductClickListener {
        void onProductClick(Product product, View sharedImageView);
        void onFavoriteClick(Product product);
    }

    // ─── Fields ──────────────────────────────────────────────────────────────────

    private final Context context;
    private List<Product> productList;
    private List<Product> originalProductList;
    private OnProductClickListener listener;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public ProductAdapter(Context context) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.originalProductList = new ArrayList<>();
    }

    public ProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.originalProductList = new ArrayList<>();
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
        this.originalProductList = products != null ? new ArrayList<>(products) : new ArrayList<>();
        
        // Tampilkan maksimal 30 item untuk beranda (default view)
        if (products != null && products.size() > 30) {
            this.productList = new ArrayList<>(products.subList(0, 30));
        } else {
            this.productList = products != null ? new ArrayList<>(products) : new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    /**
     * Menambah produk ke list yang sudah ada (untuk pagination).
     */
    public void addProducts(List<Product> products) {
        if (products != null) {
            int startPos = productList.size();
            productList.addAll(products);
            originalProductList.addAll(products);
            notifyItemRangeInserted(startPos, products.size());
        }
    }

    public void clearProducts() {
        productList.clear();
        originalProductList.clear();
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            productList.clear();
            // Jika pencarian kosong, kembali tampilkan hanya 30 item pertama
            if (originalProductList.size() > 30) {
                productList.addAll(originalProductList.subList(0, 30));
            } else {
                productList.addAll(originalProductList);
            }
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            List<Product> filteredList = new ArrayList<>();
            for (Product product : originalProductList) {
                if (product.getName() != null && product.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(product);
                }
            }
            productList.clear();
            productList.addAll(filteredList);
        }
        notifyDataSetChanged();
    }

    // ─── Filter & Sort ──────────────────────────────────────────────────────────

    public static final int SORT_PRICE_ASC = 0;
    public static final int SORT_PRICE_DESC = 1;
    public static final int SORT_RATING_ASC = 2;
    public static final int SORT_RATING_DESC = 3;

    public void sortProducts(int sortType) {
        Comparator<Product> comparator = null;
        switch (sortType) {
            case SORT_PRICE_ASC:
                comparator = (p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice());
                break;
            case SORT_PRICE_DESC:
                comparator = (p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice());
                break;
            case SORT_RATING_ASC:
                comparator = (p1, p2) -> Double.compare(p1.getRating(), p2.getRating());
                break;
            case SORT_RATING_DESC:
                comparator = (p1, p2) -> Double.compare(p2.getRating(), p1.getRating());
                break;
        }

        if (comparator != null) {
            Collections.sort(originalProductList, comparator);
            
            // Setelah sorting, update productList. 
            // Jika sedang tidak mencari, tampilkan 30 teratas.
            // Jika sedang mencari, biarkan filter yang menangani atau tampilkan semua hasil sort?
            // Kita asumsikan update tampilan utama:
            productList.clear();
            if (originalProductList.size() > 30) {
                productList.addAll(originalProductList.subList(0, 30));
            } else {
                productList.addAll(originalProductList);
            }
            notifyDataSetChanged();
        }
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

            // Set unique transition name based on product ID
            androidx.core.view.ViewCompat.setTransitionName(imgProduct, "product_image_" + product.getProductId());

            // Klik item → buka DetailActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product, imgProduct);
            });

            // Klik tombol favorit
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    if (listener != null) listener.onFavoriteClick(product);
                });
            }
        }

        private String formatPrice(double price) {
            return LanguageManager.formatPrice(context, price);
        }
    }
}
