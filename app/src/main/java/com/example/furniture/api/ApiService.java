package com.example.furniture.api;

import com.example.furniture.model.AutoCompleteResponse;
import com.example.furniture.model.CategoryResponse;
import com.example.furniture.model.ProductDetailResponse;
import com.example.furniture.model.ProductResponse;
import com.example.furniture.model.QnaResponse;
import com.example.furniture.model.ReviewResponse;
import com.example.furniture.model.StoreResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface Retrofit untuk endpoint Kohls API (RapidAPI by Api Dojo).
 * Host: kohls.p.rapidapi.com
 */
public interface ApiService {

    /**
     * GET categories/list
     * Mengambil seluruh tree kategori yang tersedia.
     */
    @GET("categories/list")
    Call<CategoryResponse> getCategories();

    /**
     * GET products/list
     * Mengambil daftar produk berdasarkan kategori.
     *
     * @param categoryId ID kategori dari endpoint categories/list
     * @param limit      jumlah item per halaman (default 24, max 48)
     * @param offset     offset untuk pagination (mis. 0, 24, 48, ...)
     * @param sortId     0 = Featured, 4 = Price Low-High, 5 = Price High-Low, dll
     */
    @GET("products/list")
    Call<ProductResponse> getProducts(
            @Query("categoryID") String categoryId,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("sortID") int sortId
    );

    /**
     * GET products/search-by-barcode
     *
     * @param upc nilai UPC hasil scan barcode
     */
    @GET("products/search-by-barcode")
    Call<ProductDetailResponse> searchByBarcode(
            @Query("upc") String upc
    );

    /**
     * GET products/detail
     *
     * @param webId webID yang didapat dari response products/list
     */
    @GET("products/detail")
    Call<ProductDetailResponse> getProductDetail(
            @Query("webID") String webId
    );

    /**
     * GET reviews/list
     *
     * @param productId webID produk
     * @param limit     jumlah review yang diambil
     * @param offset    offset pagination
     * @param sort      contoh: "SubmissionTime:desc", "Rating:desc"
     */
    @GET("reviews/list")
    Call<ReviewResponse> getReviews(
            @Query("ProductId") String productId,
            @Query("Limit") int limit,
            @Query("Offset") int offset,
            @Query("Sort") String sort
    );

    /**
     * GET qnas/list
     *
     * @param productId webID produk
     * @param limit     jumlah QnA yang diambil
     * @param offset    offset pagination
     * @param sort      contoh: "SubmissionTime:desc"
     */
    @GET("qnas/list")
    Call<QnaResponse> getQnas(
            @Query("ProductId") String productId,
            @Query("Limit") int limit,
            @Query("Offset") int offset,
            @Query("Sort") String sort
    );

    /**
     * GET stores/list
     *
     * @param latitude  koordinat lokasi pencarian
     * @param longitude koordinat lokasi pencarian
     * @param radius    radius pencarian dalam mil
     */
    @GET("stores/list")
    Call<StoreResponse> getStores(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radius") int radius
    );

    /**
     * GET auto-complete (DEPRECATING)
     * Mengembalikan daftar saran pencarian berdasarkan query.
     *
     * @param query kata kunci yang diketik user
     */
    @GET("auto-complete")
    Call<AutoCompleteResponse> getAutoComplete(
            @Query("query") String query
    );
}
