package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response wrapper untuk endpoint products/detail.
 * Struktur JSON: { "payload": { "product": {...} } }
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

    public static class Payload {

        @SerializedName("product")
        private Product product;

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }
    }
}
