package com.example.furniture.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    /**
     * Mengubah string input menjadi hash SHA-256.
     * Digunakan untuk mengenkripsi password sebelum disimpan di SQLite.
     *
     * @param input String yang akan di-hash
     * @return String hash (hexadecimal) atau input awal jika terjadi error
     */
    public static String sha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return input; // Fallback to raw string if algorithm missing (very unlikely)
        }
    }
}
