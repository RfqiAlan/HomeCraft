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
import com.example.furniture.model.CartItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView untuk daftar produk di keranjang.
 * Digunakan di CartFragment.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    // ─── Interface ───────────────────────────────────────────────────────────────

    public interface OnCartActionListener {
        void onIncrease(CartItem item);
        void onDecrease(CartItem item);
        void onRemove(CartItem item);
    }

    // ─── Fields ──────────────────────────────────────────────────────────────────

    private final Context context;
    private List<CartItem> cartList;
    private final OnCartActionListener listener;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public CartAdapter(Context context, OnCartActionListener listener) {
        this.context = context;
        this.cartList = new ArrayList<>();
        this.listener = listener;
    }

    // ─── Public Methods ──────────────────────────────────────────────────────────

    public void setCartItems(List<CartItem> items) {
        this.cartList = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(CartItem item) {
        int index = cartList.indexOf(item);
        if (index != -1) {
            cartList.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void updateItem(CartItem updatedItem) {
        for (int i = 0; i < cartList.size(); i++) {
            if (cartList.get(i).getProductId().equals(updatedItem.getProductId())) {
                cartList.set(i, updatedItem);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public boolean isEmpty() {
        return cartList.isEmpty();
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────────

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(cartList.get(position));
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class CartViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName;
        private final TextView tvPrice;
        private final TextView tvQuantity;
        private final TextView tvTotalPrice;
        private final ImageButton btnIncrease;
        private final ImageButton btnDecrease;
        private final ImageButton btnRemove;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct  = itemView.findViewById(R.id.img_cart_product);
            tvName      = itemView.findViewById(R.id.tv_cart_name);
            tvPrice     = itemView.findViewById(R.id.tv_cart_price);
            tvQuantity  = itemView.findViewById(R.id.tv_cart_quantity);
            tvTotalPrice = itemView.findViewById(R.id.tv_cart_total_price);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnRemove   = itemView.findViewById(R.id.btn_remove_cart);
        }

        void bind(CartItem item) {
            tvName.setText(item.getName());
            tvPrice.setText(formatPrice(item.getPrice()));
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvTotalPrice.setText(formatPrice(item.getTotalPrice()));

            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(imgProduct);

            // Tambah quantity
            if (btnIncrease != null) {
                btnIncrease.setOnClickListener(v -> {
                    if (listener != null) listener.onIncrease(item);
                });
            }

            // Kurangi quantity
            if (btnDecrease != null) {
                btnDecrease.setOnClickListener(v -> {
                    if (listener != null) listener.onDecrease(item);
                });
            }

            // Hapus dari cart
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    if (listener != null) listener.onRemove(item);
                });
            }
        }

        private String formatPrice(double price) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
            return nf.format(price);
        }
    }
}
