package com.example.furniture.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.furniture.model.FavoriteItem;
import com.example.furniture.model.Product;
import com.example.furniture.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DAO untuk mengelola data wishlist/favorit.
 */
public class FavoriteDao {

    private final DatabaseHelper dbHelper;

    public FavoriteDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Menambah produk ke daftar favorit.
     * Jika produk sudah ada, diabaikan (CONFLICT_IGNORE).
     */
    public void addFavorite(Product product) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_id", product.getProductId());
        values.put("name", product.getName());
        values.put("image_url", product.getImageUrl());
        values.put("price", product.getPrice());
        values.put("created_at", getCurrentDateTime());

        db.insertWithOnConflict(Constants.TABLE_FAVORITES,
                null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * Menghapus produk dari favorit berdasarkan product_id.
     */
    public void removeFavorite(String productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Constants.TABLE_FAVORITES,
                "product_id = ?", new String[]{productId});
    }

    /**
     * Mengambil semua produk favorit, diurutkan dari terbaru.
     */
    public List<FavoriteItem> getAllFavorites() {
        List<FavoriteItem> favorites = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(Constants.TABLE_FAVORITES,
                null, null, null, null, null, "id DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                favorites.add(cursorToFavoriteItem(cursor));
            }
            cursor.close();
        }
        return favorites;
    }

    /**
     * Mengecek apakah produk sudah ada di favorit.
     */
    public boolean isFavorite(String productId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(Constants.TABLE_FAVORITES,
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

    private FavoriteItem cursorToFavoriteItem(Cursor cursor) {
        FavoriteItem item = new FavoriteItem();
        item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        item.setProductId(cursor.getString(cursor.getColumnIndexOrThrow("product_id")));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        item.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        return item;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
