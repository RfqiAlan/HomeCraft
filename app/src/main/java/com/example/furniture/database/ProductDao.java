package com.example.furniture.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.furniture.model.Product;
import com.example.furniture.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) untuk cache produk dari API.
 * Semua operasi database dijalankan di background thread oleh pemanggil.
 */
public class ProductDao {

    private final DatabaseHelper dbHelper;

    public ProductDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Menyimpan list produk dari API ke SQLite cache.
     * Jika produk sudah ada (berdasarkan product_id), akan diabaikan (INSERT OR IGNORE).
     */
    public void insertProducts(List<Product> products) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Product p : products) {
                ContentValues values = new ContentValues();
                values.put("product_id", p.getProductId());
                values.put("name", p.getName());
                values.put("image_url", p.getImageUrl());
                values.put("price", p.getPrice());
                values.put("description", p.getDescription());
                values.put("category", p.getCategory());
                values.put("rating", p.getRating());
                values.put("review_count", p.getReviewCount());
                db.insertWithOnConflict(Constants.TABLE_PRODUCTS_CACHE,
                        null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Mengambil semua produk yang tersimpan di cache.
     */
    public List<Product> getCachedProducts() {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(Constants.TABLE_PRODUCTS_CACHE,
                null, null, null, null, null, "id DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
            cursor.close();
        }
        return products;
    }

    /**
     * Mengambil satu produk berdasarkan product_id dari cache.
     */
    public Product getProductById(String productId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(Constants.TABLE_PRODUCTS_CACHE,
                null, "product_id = ?", new String[]{productId},
                null, null, null, "1");

        Product product = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                product = cursorToProduct(cursor);
            }
            cursor.close();
        }
        return product;
    }

    /**
     * Menghapus semua data cache produk.
     */
    public void clearProducts() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Constants.TABLE_PRODUCTS_CACHE, null, null);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private Product cursorToProduct(Cursor cursor) {
        Product p = new Product();
        p.setProductId(cursor.getString(cursor.getColumnIndexOrThrow("product_id")));
        p.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        p.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        p.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        p.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        p.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        p.setRating(cursor.getDouble(cursor.getColumnIndexOrThrow("rating")));
        p.setReviewCount(cursor.getInt(cursor.getColumnIndexOrThrow("review_count")));
        return p;
    }
}
