package com.example.furniture.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.furniture.utils.Constants;

/**
 * SQLiteOpenHelper untuk membuat dan mengelola database FurniSpace.
 * Membuat semua tabel yang dibutuhkan: products_cache, categories_cache,
 * favorites, cart, orders, order_details.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ─── CREATE TABLE ────────────────────────────────────────────────────────────

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "email TEXT UNIQUE NOT NULL, " +
            "password TEXT NOT NULL, " +
            "address TEXT DEFAULT '', " +
            "phone_number TEXT DEFAULT '', " +
            "default_payment TEXT DEFAULT ''" +
            ");"; 

    private static final String CREATE_TABLE_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_PRODUCTS_CACHE + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "product_id TEXT UNIQUE NOT NULL, " +
            "name TEXT, " +
            "image_url TEXT, " +
            "price REAL, " +
            "description TEXT, " +
            "category TEXT, " +
            "rating REAL, " +
            "review_count INTEGER" +
            ");";

    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_CATEGORIES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "category_id TEXT UNIQUE NOT NULL, " +
            "name TEXT, " +
            "seo_url TEXT, " +
            "type TEXT, " +
            "parent_id TEXT" +
            ");";

    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_FAVORITES + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "product_id TEXT UNIQUE NOT NULL, " +
            "name TEXT, " +
            "image_url TEXT, " +
            "price REAL, " +
            "created_at TEXT" +
            ");";

    private static final String CREATE_TABLE_CART =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_CART + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "product_id TEXT UNIQUE NOT NULL, " +
            "name TEXT, " +
            "image_url TEXT, " +
            "price REAL, " +
            "quantity INTEGER DEFAULT 1" +
            ");";

    private static final String CREATE_TABLE_ORDERS =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_ORDERS + " (" +
            "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER DEFAULT 0, " +
            "total_price REAL, " +
            "payment_method TEXT, " +
            "order_status TEXT, " +
            "shipping_address TEXT DEFAULT '', " +
            "created_at TEXT, " +
            "FOREIGN KEY(user_id) REFERENCES users(id)" +
            ");";

    private static final String CREATE_TABLE_ORDER_DETAILS =
            "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_ORDER_DETAILS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "order_id INTEGER, " +
            "product_id TEXT, " +
            "name TEXT, " +
            "price REAL, " +
            "quantity INTEGER, " +
            "FOREIGN KEY(order_id) REFERENCES " + Constants.TABLE_ORDERS + "(order_id)" +
            ");";

    // ─── Singleton ───────────────────────────────────────────────────────────────

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_PRODUCTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_CART);
        db.execSQL(CREATE_TABLE_ORDERS);
        db.execSQL(CREATE_TABLE_ORDER_DETAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop semua tabel dan buat ulang saat versi DB naik
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_ORDER_DETAILS);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CART);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_PRODUCTS_CACHE);
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
