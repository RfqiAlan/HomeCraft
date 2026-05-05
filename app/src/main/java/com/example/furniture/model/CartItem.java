package com.example.furniture.model;

/**
 * Model untuk item di keranjang belanja.
 * Data disimpan ke tabel SQLite 'cart'.
 */
public class CartItem {

    private int id;
    private String productId;
    private String name;
    private double price;
    private String imageUrl;
    private int quantity;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public CartItem() {}

    public CartItem(String productId, String name, double price, String imageUrl, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────────

    /**
     * Hitung total harga untuk item ini (price * quantity).
     */
    public double getTotalPrice() {
        return price * quantity;
    }

    public void increaseQuantity() {
        this.quantity++;
    }

    public void decreaseQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
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

    public int getQuantity() {
        return quantity;
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

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{productId='" + productId + "', name='" + name
                + "', qty=" + quantity + ", total=" + getTotalPrice() + "}";
    }
}
