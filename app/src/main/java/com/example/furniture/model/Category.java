package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model untuk kategori produk furniture.
 * Mendukung nested subcategory (kategori di dalam kategori).
 */
public class Category {

    @SerializedName("ID")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("seoURL")
    private String seoUrl;

    @SerializedName("type")
    private String type;

    @SerializedName("categories")
    private List<Category> subCategories;

    // ─── Getters ────────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSeoUrl() {
        return seoUrl;
    }

    public String getType() {
        return type;
    }

    public List<Category> getSubCategories() {
        return subCategories;
    }

    // ─── Setters ─────────────────────────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeoUrl(String seoUrl) {
        this.seoUrl = seoUrl;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSubCategories(List<Category> subCategories) {
        this.subCategories = subCategories;
    }

    @Override
    public String toString() {
        return "Category{id='" + id + "', name='" + name + "'}";
    }
}
