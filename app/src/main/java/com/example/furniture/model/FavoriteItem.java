package com.example.furniture.model;

/**
 * Model untuk produk favorit/wishlist.
 * Data disimpan ke tabel SQLite 'favorites'.
 */
public class FavoriteItem {

    private int id;
    private String productId;
    private String name;
    private double price;
    private String imageUrl;
    private String createdAt;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public FavoriteItem() {}

    public FavoriteItem(String productId, String name, double price, String imageUrl, String createdAt) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // ─── Setters ─────────────────────────────────────────────────────────────────

    public void setId(int id) {
        this.id = id;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FavoriteItem{productId='" + productId + "', name='" + name + "'}";
    }
}
