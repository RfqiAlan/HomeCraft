package com.example.furniture.api;

import androidx.annotation.NonNull;

import com.example.furniture.model.Product;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Custom Gson deserializer untuk memetakan JSON produk dari Kohls API
 * ({@code webID}, {@code productTitle}, {@code image.url}, {@code prices[0].salePrice.minPrice},
 * {@code rating.avgRating}, dst) ke POJO {@link Product} yang lebih ringkas.
 *
 * Deserializer ini menangani dua bentuk response:
 *  1. Format list   → objek produk dari endpoint {@code products/list}
 *  2. Format detail → objek produk dari endpoint {@code products/detail} &
 *                     {@code products/search-by-barcode}
 */
public class ProductDeserializer implements JsonDeserializer<Product> {

    @Override
    public Product deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        if (json == null || !json.isJsonObject()) {
            return null;
        }
        JsonObject obj = json.getAsJsonObject();
        Product product = new Product();

        // ─── ID & Title ─────────────────────────────────────────────────────────
        product.setProductId(optString(obj, "webID"));
        product.setName(optString(obj, "productTitle"));

        // ─── Image URLs ─────────────────────────────────────────────────────────
        // products/list  → image: { url, width, height }
        // products/detail → images: [ { url, ... }, ... ]
        String imageUrl = null;
        java.util.List<String> imageUrls = new java.util.ArrayList<>();

        if (obj.has("image") && obj.get("image").isJsonObject()) {
            imageUrl = optString(obj.getAsJsonObject("image"), "url");
            if (imageUrl != null) imageUrls.add(imageUrl);
        } else if (obj.has("images") && obj.get("images").isJsonArray()) {
            JsonArray images = obj.getAsJsonArray("images");
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).isJsonObject()) {
                    String url = optString(images.get(i).getAsJsonObject(), "url");
                    if (url != null && !url.isEmpty()) {
                        imageUrls.add(url);
                        if (imageUrl == null) imageUrl = url; // gunakan foto pertama sebagai thumbnail
                    }
                }
            }
        }
        product.setImageUrl(imageUrl);
        product.setImageUrls(imageUrls);

        // ─── Price ──────────────────────────────────────────────────────────────
        // Prefer salePrice.minPrice; fallback ke regularPrice.minPrice.
        double price = extractPrice(obj);
        product.setPrice(price);

        // ─── Description ────────────────────────────────────────────────────────
        // Hanya ada di products/detail & search-by-barcode.
        if (obj.has("description") && obj.get("description").isJsonObject()) {
            JsonObject desc = obj.getAsJsonObject("description");
            String shortDesc = optString(desc, "shortDescription");
            String longDesc  = optString(desc, "longDescription");
            product.setDescription(shortDesc != null && !shortDesc.isEmpty() ? shortDesc : longDesc);
        }

        // ─── Category / Brand ───────────────────────────────────────────────────
        // Kohls tidak mengembalikan nama kategori di objek produk, gunakan brand
        // sebagai label kategori agar UI tetap menampilkan info yang berguna.
        String brand = optString(obj, "brand");
        product.setCategory(brand);

        // ─── Rating & Review count ─────────────────────────────────────────────
        // products/list  → rating: { avgRating, count }
        // products/detail → avgRating (string), ratingCount (int)
        double avgRating = 0.0;
        int reviewCount  = 0;

        if (obj.has("rating") && obj.get("rating").isJsonObject()) {
            JsonObject rating = obj.getAsJsonObject("rating");
            avgRating   = optDouble(rating, "avgRating");
            reviewCount = optInt(rating, "count");
        } else {
            avgRating   = optDouble(obj, "avgRating");
            reviewCount = optInt(obj, "ratingCount");
        }
        product.setRating(avgRating);
        product.setReviewCount(reviewCount);

        return product;
    }

    // ─── Price helpers ──────────────────────────────────────────────────────────

    private double extractPrice(@NonNull JsonObject obj) {
        // Format products/list: "prices": [ { "salePrice": {...}, "regularPrice": {...} }, ... ]
        if (obj.has("prices") && obj.get("prices").isJsonArray()) {
            JsonArray prices = obj.getAsJsonArray("prices");
            if (prices.size() > 0 && prices.get(0).isJsonObject()) {
                return pickPrice(prices.get(0).getAsJsonObject());
            }
        }
        // Format products/detail: "price": { "salePrice": {...}, "regularPrice": {...} }
        if (obj.has("price") && obj.get("price").isJsonObject()) {
            return pickPrice(obj.getAsJsonObject("price"));
        }
        return 0.0;
    }

    private double pickPrice(@NonNull JsonObject priceObj) {
        double sale = minPrice(priceObj, "salePrice");
        if (sale > 0) return sale;
        double regular = minPrice(priceObj, "regularPrice");
        if (regular > 0) return regular;
        double lowest = minPrice(priceObj, "lowestApplicablePrice");
        return lowest;
    }

    private double minPrice(@NonNull JsonObject priceObj, String key) {
        if (!priceObj.has(key) || !priceObj.get(key).isJsonObject()) return 0.0;
        JsonObject inner = priceObj.getAsJsonObject(key);
        return optDouble(inner, "minPrice");
    }

    // ─── Primitive helpers ──────────────────────────────────────────────────────

    private static String optString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static double optDouble(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0.0;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static int optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }
}
