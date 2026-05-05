package com.example.furniture.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.furniture.model.CartItem;
import com.example.furniture.model.Order;
import com.example.furniture.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DAO untuk mengelola data pesanan.
 */
public class OrderDao {

    private final DatabaseHelper dbHelper;

    public OrderDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Membuat order baru dan menyimpannya ke tabel orders.
     *
     * @param userId          ID user yang melakukan order
     * @param totalPrice      Total harga pesanan
     * @param paymentMethod   Metode pembayaran yang dipilih
     * @param status          Status awal order (contoh: Order.STATUS_WAITING)
     * @param shippingAddress Alamat pengiriman user
     * @return ID order yang baru dibuat, atau -1 jika gagal
     */
    public long createOrder(int userId, double totalPrice, String paymentMethod,
                             String status, String shippingAddress) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("total_price", totalPrice);
        values.put("payment_method", paymentMethod);
        values.put("order_status", status);
        values.put("shipping_address", shippingAddress);
        values.put("created_at", getCurrentDateTime());

        return db.insert(Constants.TABLE_ORDERS, null, values);
    }

    /**
     * Menyimpan detail item dari cart ke tabel order_details.
     *
     * @param orderId   ID order yang sudah dibuat
     * @param cartItems List item dari cart
     */
    public void insertOrderDetail(long orderId, List<CartItem> cartItems) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (CartItem item : cartItems) {
                ContentValues values = new ContentValues();
                values.put("order_id", orderId);
                values.put("product_id", item.getProductId());
                values.put("name", item.getName());
                values.put("price", item.getPrice());
                values.put("quantity", item.getQuantity());
                db.insert(Constants.TABLE_ORDER_DETAILS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Mengambil semua riwayat pesanan, diurutkan dari terbaru.
     */
    public List<Order> getOrders() {
        List<Order> orders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(Constants.TABLE_ORDERS,
                null, null, null, null, null, "order_id DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Order order = new Order();
                order.setOrderId(cursor.getInt(cursor.getColumnIndexOrThrow("order_id")));
                order.setTotalPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("total_price")));
                order.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow("payment_method")));
                order.setOrderStatus(cursor.getString(cursor.getColumnIndexOrThrow("order_status")));
                order.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                orders.add(order);
            }
            cursor.close();
        }
        return orders;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
