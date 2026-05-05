package com.example.furniture.utils;

/**
 * Konstanta global untuk seluruh aplikasi FurniSpace.
 * CATATAN: Ganti BASE_URL, API_KEY, dan API_HOST sesuai endpoint RapidAPI yang digunakan.
 */
public class Constants {

    // ─── API Configuration ───────────────────────────────────────────────────────
    // PENTING: Isi dengan base URL, API key, dan host dari RapidAPI kamu
    public static final String BASE_URL = "https://apidojo-walmart-labs-v1.p.rapidapi.com/";
    public static final String API_KEY  = "YOUR_RAPIDAPI_KEY_HERE";
    public static final String API_HOST = "apidojo-walmart-labs-v1.p.rapidapi.com";

    // ─── Intent Extra Keys ───────────────────────────────────────────────────────
    public static final String EXTRA_PRODUCT_ID = "product_id";

    // ─── SQLite Database ─────────────────────────────────────────────────────────
    public static final String DB_NAME    = "furnispace.db";
    public static final int    DB_VERSION = 1;

    // ─── SQLite Table Names ──────────────────────────────────────────────────────
    public static final String TABLE_PRODUCTS_CACHE  = "products_cache";
    public static final String TABLE_CATEGORIES      = "categories_cache";
    public static final String TABLE_FAVORITES       = "favorites";
    public static final String TABLE_CART            = "cart";
    public static final String TABLE_ORDERS          = "orders";
    public static final String TABLE_ORDER_DETAILS   = "order_details";

    // ─── SharedPreferences ───────────────────────────────────────────────────────
    public static final String PREF_NAME       = "furnispace_prefs";
    public static final String PREF_THEME_MODE = "theme_mode";
    public static final String THEME_DARK      = "dark";
    public static final String THEME_LIGHT     = "light";

    // ─── Category ────────────────────────────────────────────────────────────────
    // ID kategori Furniture dari API (sesuai data RapidAPI)
    public static final String FURNITURE_CATEGORY_ID   = "1350311459149";
    public static final String FURNITURE_CATEGORY_NAME = "Furniture";

    // ─── Payment Methods ─────────────────────────────────────────────────────────
    public static final String[] PAYMENT_METHODS = {
            "Cash on Delivery",
            "Bank Transfer",
            "E-Wallet",
            "Virtual Account",
            "Credit/Debit Card"
    };

    // ─── Prevent Instantiation ───────────────────────────────────────────────────
    private Constants() {}
}
