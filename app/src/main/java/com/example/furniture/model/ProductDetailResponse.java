package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response wrapper untuk endpoint {@code products/detail} dan
 * {@code products/search-by-barcode}.
 * Struktur JSON: {@code { "payload": { "products": [ {...} ] } }}
 */
public class ProductDetailResponse {

    @SerializedName("payload")
    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    /**
     * Helper: ambil produk pertama bila tersedia, atau null.
     * Kohls selalu mengembalikan list meskipun hanya 1 produk.
     */
    public Product getProduct() {
        if (payload == null || payload.getProducts() == null || payload.getProducts().isEmpty()) {
            return null;
        }
        return payload.getProducts().get(0);
    }

    public static class Payload {

        @SerializedName("products")
        private List<Product> products;

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }
    }
}
