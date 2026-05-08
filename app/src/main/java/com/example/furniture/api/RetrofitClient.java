package com.example.furniture.api;

import com.example.furniture.model.Product;
import com.example.furniture.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client untuk Kohls API (RapidAPI).
 * - Menyisipkan header x-rapidapi-key & x-rapidapi-host via interceptor
 * - Menggunakan custom Gson dengan {@link ProductDeserializer}
 */
public class RetrofitClient {

    private static Retrofit retrofit;

    /**
     * Mengembalikan instance ApiService yang siap digunakan (singleton).
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(buildOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(buildGson()))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    // ─── Gson ────────────────────────────────────────────────────────────────────

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Product.class, new ProductDeserializer())
                .setLenient()
                .create();
    }

    // ─── OkHttp ──────────────────────────────────────────────────────────────────

    private static OkHttpClient buildOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // Inject header RapidAPI untuk semua request
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("x-rapidapi-key", Constants.API_KEY)
                            .header("x-rapidapi-host", Constants.API_HOST)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .addInterceptor(loggingInterceptor)
                .build();
    }

    private RetrofitClient() {}
}
