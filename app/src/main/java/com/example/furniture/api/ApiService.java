package com.example.furniture.api;

import com.example.furniture.model.CategoryResponse;
import com.example.furniture.model.ProductDetailResponse;
import com.example.furniture.model.ProductResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface Retrofit untuk mendefinisikan endpoint API.
 * CATATAN: Sesuaikan nama @Query parameter dengan dokumentasi RapidAPI yang digunakan.
 */
public interface ApiService {

    /**
     * Mengambil daftar kategori produk.
     * Endpoint: GET categories/list
     */
    @GET("categories/list")
    Call<CategoryResponse> getCategories();

    /**
     * Mengambil daftar produk berdasarkan kategori.
     * Endpoint: GET products/list
     *
     * @param categoryId ID kategori (contoh: "1350311459149" untuk Furniture)
     * @param page       Nomor halaman untuk pagination (opsional)
     */
    @GET("products/list")
    Call<ProductResponse> getProducts(
            @Query("categoryId") String categoryId,
            @Query("page") int page
    );

    /**
     * Mengambil detail satu produk.
     * Endpoint: GET products/detail
     *
     * @param productId ID produk yang ingin ditampilkan
     */
    @GET("products/detail")
    Call<ProductDetailResponse> getProductDetail(
            @Query("productId") String productId
    );
}
