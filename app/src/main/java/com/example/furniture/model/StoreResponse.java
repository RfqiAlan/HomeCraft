package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response wrapper untuk endpoint {@code stores/list}.
 * Struktur JSON: {@code { "payload": { "stores": [ {...} ] } }}
 */
public class StoreResponse {

    @SerializedName("payload")
    private Payload payload;

    public Payload getPayload() { return payload; }
    public void setPayload(Payload payload) { this.payload = payload; }

    public List<Store> getStores() {
        return payload != null ? payload.getStores() : null;
    }

    public static class Payload {
        @SerializedName("stores")
        private List<Store> stores;

        public List<Store> getStores() { return stores; }
        public void setStores(List<Store> stores) { this.stores = stores; }
    }
}
