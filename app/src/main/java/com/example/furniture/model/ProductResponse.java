package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper untuk endpoint products/list.
 * Struktur JSON: { "payload": { "products": [...] } }
 * CATATAN: Jika struktur JSON berbeda, sesuaikan inner class Payload-nya.
 */
public class ProductResponse {

    @SerializedName("payload")
    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
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
