package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper untuk endpoint categories/list.
 * Struktur JSON: { "payload": { "categories": [...] } }
 */
public class CategoryResponse {

    @SerializedName("payload")
    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public static class Payload {

        @SerializedName("categories")
        private List<Category> categories;

        public List<Category> getCategories() {
            return categories;
        }

        public void setCategories(List<Category> categories) {
            this.categories = categories;
        }
    }
}
