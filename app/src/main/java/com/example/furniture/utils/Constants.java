package com.example.furniture.utils;

/**
 * Konstanta global untuk seluruh aplikasi HomeCraft.
 * Base URL, host, dan key mengarah ke Kohls API (RapidAPI by Api Dojo).
 */
public class Constants {

    // ─── API Configuration (Kohls via RapidAPI) ──────────────────────────────────
    public static final String BASE_URL = "https://kohls.p.rapidapi.com/";
    public static final String API_HOST = "kohls.p.rapidapi.com";

    // PENTING: Key RapidAPI. Jangan commit key asli ke repo publik.
    public static final String API_KEY  =
            "aa06c32967msh6c6dee9e1ada3e4p125fcajsn142b7625bed6";

    // ─── Intent Extra Keys ───────────────────────────────────────────────────────
    public static final String EXTRA_PRODUCT_ID = "product_id";

    // ─── SQLite Database ─────────────────────────────────────────────────────────
    public static final String DB_NAME    = "furnispace.db";
    public static final int    DB_VERSION = 2;

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
    // ID kategori "Furniture" hasil pencarian pada endpoint categories/list Kohls.
    public static final String FURNITURE_CATEGORY_ID   = "1381075460323";
    public static final String FURNITURE_CATEGORY_NAME = "Furniture";

    // ─── Products/list defaults ─────────────────────────────────────────────────
    public static final int DEFAULT_LIMIT    = 24;
    public static final int DEFAULT_OFFSET   = 0;
    public static final int DEFAULT_SORT_ID  = 0;

    // ─── Reviews / QnAs defaults ────────────────────────────────────────────────
    public static final int    DEFAULT_REVIEW_LIMIT = 10;
    public static final String DEFAULT_REVIEW_SORT  = "SubmissionTime:desc";

    // ─── Stores defaults ────────────────────────────────────────────────────────
    public static final double DEFAULT_STORE_LATITUDE  = 33.9733;
    public static final double DEFAULT_STORE_LONGITUDE = -118.2487;
    public static final int    DEFAULT_STORE_RADIUS    = 25;

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
