package com.example.furniture.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.furniture.model.User;

/**
 * DAO untuk mengelola data user (registrasi, login, profil).
 */
public class UserDao {

    private static final String TABLE = "users";

    private final DatabaseHelper dbHelper;

    public UserDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ─── Register ────────────────────────────────────────────────────────────────

    /**
     * Mendaftarkan user baru.
     *
     * @return ID user baru, atau -1 jika email sudah dipakai
     */
    public long register(User user) {
        // Cek apakah email sudah terdaftar
        if (isEmailRegistered(user.getEmail())) {
            return -1;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", user.getName());
        values.put("email", user.getEmail());
        values.put("password", user.getPassword());
        values.put("address", user.getAddress() != null ? user.getAddress() : "");
        values.put("phone_number", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        values.put("default_payment", user.getDefaultPayment() != null ? user.getDefaultPayment() : "");

        return db.insert(TABLE, null, values);
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    /**
     * Validasi login berdasarkan email dan password.
     *
     * @return User jika berhasil, null jika gagal
     */
    public User login(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE,
                null,
                "email = ? AND password = ?",
                new String[]{email, password},
                null, null, null, "1");

        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
            cursor.close();
        }
        return user;
    }

    // ─── Get ─────────────────────────────────────────────────────────────────────

    /**
     * Mengambil data user berdasarkan ID (untuk load profil di checkout).
     */
    public User getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE,
                null,
                "id = ?",
                new String[]{String.valueOf(userId)},
                null, null, null, "1");

        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
            cursor.close();
        }
        return user;
    }

    /**
     * Mengambil data user berdasarkan email (dipakai untuk biometric login).
     * Tidak memerlukan password karena otentikasi sudah dilakukan hardware sidik jari.
     *
     * @return User jika email ditemukan, null jika tidak ada
     */
    public User getUserByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE,
                null,
                "email = ?",
                new String[]{email},
                null, null, null, "1");

        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
            cursor.close();
        }
        return user;
    }

    // ─── Update Profil ───────────────────────────────────────────────────────────

    /**
     * Update alamat pengiriman user.
     */
    public void updateAddress(int userId, String address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("address", address);
        db.update(TABLE, values, "id = ?", new String[]{String.valueOf(userId)});
    }

    /**
     * Update metode pembayaran default user.
     */
    public void updateDefaultPayment(int userId, String paymentMethod) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("default_payment", paymentMethod);
        db.update(TABLE, values, "id = ?", new String[]{String.valueOf(userId)});
    }

    /**
     * Update alamat dan payment default sekaligus (dipanggil saat checkout selesai).
     */
    public void updateProfile(int userId, String address, String phoneNumber, String paymentMethod) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("phone_number", phoneNumber);
        values.put("default_payment", paymentMethod);
        db.update(TABLE, values, "id = ?", new String[]{String.valueOf(userId)});
    }

    // ─── Check ───────────────────────────────────────────────────────────────────

    public boolean isEmailRegistered(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE,
                new String[]{"id"},
                "email = ?",
                new String[]{email},
                null, null, null, "1");

        boolean exists = false;
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        return exists;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
        user.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
        user.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow("phone_number")));
        user.setDefaultPayment(cursor.getString(cursor.getColumnIndexOrThrow("default_payment")));
        return user;
    }
}
