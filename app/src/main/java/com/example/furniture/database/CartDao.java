package com.example.furniture.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.furniture.model.CartItem;
import com.example.furniture.model.Product;
import com.example.furniture.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk mengelola data keranjang belanja.
 */
public class CartDao {

    private final DatabaseHelper dbHelper;

    public CartDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Menambah produk ke cart.
     * Jika sudah ada, tambahkan quantity-nya.
     */
    public void addToCart(Product product) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (isInCart(product.getProductId())) {
            // Produk sudah ada → tambah quantity
            increaseQuantity(product.getProductId());
        } else {
            ContentValues values = new ContentValues();
            values.put("product_id", product.getProductId());
            values.put("name", product.getName());
            values.put("image_url", product.getImageUrl());
            values.put("price", product.getPrice());
            values.put("quantity", 1);
            db.insert(Constants.TABLE_CART, null, values);
        }
    }

    /**
     * Mengambil semua item di cart.
     */
    public List<CartItem> getCartItems() {
        List<CartItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(Constants.TABLE_CART,
                null, null, null, null, null, "id ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                items.add(cursorToCartItem(cursor));
            }
            cursor.close();
        }
        return items;
    }

    /**
     * Menambah quantity item di cart sebesar 1.
     */
    public void increaseQuantity(String productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + Constants.TABLE_CART +
                " SET quantity = quantity + 1 WHERE product_id = ?",
                new String[]{productId});
    }

    /**
     * Mengurangi quantity item di cart sebesar 1.
     * Jika quantity sudah 1, tidak dikurangi lagi (hapus manual via removeFromCart).
     */
    public void decreaseQuantity(String productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Hanya kurangi jika quantity > 1
        db.execSQL("UPDATE " + Constants.TABLE_CART +
                " SET quantity = quantity - 1 WHERE product_id = ? AND quantity > 1",
                new String[]{productId});
    }

    /**
     * Menghapus satu item dari cart berdasarkan product_id.
     */
    public void removeFromCart(String productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Constants.TABLE_CART,
                "product_id = ?", new String[]{productId});
    }

    /**
     * Mengosongkan seluruh isi cart (dipakai setelah checkout berhasil).
     */
    public void clearCart() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Constants.TABLE_CART, null, null);
    }

    /**
     * Menghitung total harga semua item di cart.
     */
    public double getCartTotal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(price * quantity) FROM " + Constants.TABLE_CART, null);

        double total = 0.0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
        }
        return total;
    }

    /**
     * Mengembalikan jumlah total item (quantity) di cart untuk badge Bottom Nav.
     */
    public int getCartItemCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(quantity) FROM " + Constants.TABLE_CART, null);

        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    /**
     * Mengecek apakah produk sudah ada di cart.
     */
    public boolean isInCart(String productId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(Constants.TABLE_CART,
                new String[]{"id"}, "product_id = ?",
                new String[]{productId}, null, null, null, "1");

        boolean exists = false;
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        return exists;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private CartItem cursorToCartItem(Cursor cursor) {
        CartItem item = new CartItem();
        item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        item.setProductId(cursor.getString(cursor.getColumnIndexOrThrow("product_id")));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        item.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
        return item;
    }
}
