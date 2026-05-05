package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model untuk satu produk furniture.
 * Field disesuaikan dengan endpoint products/list dari RapidAPI.
 * CATATAN: Sesuaikan nama @SerializedName dengan nama field JSON asli dari API.
 */
public class Product {

    @SerializedName("productId")
    private String productId;

    @SerializedName("title")
    private String name;

    @SerializedName("thumbnail")
    private String imageUrl;

    @SerializedName("price")
    private double price;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("rating")
    private double rating;

    @SerializedName("reviewCount")
    private int reviewCount;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public double getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    // ─── Setters ─────────────────────────────────────────────────────────────────

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    @Override
    public String toString() {
        return "Product{productId='" + productId + "', name='" + name + "', price=" + price + "}";
    }
}
