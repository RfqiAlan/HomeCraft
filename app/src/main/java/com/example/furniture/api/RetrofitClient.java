package com.example.furniture.api;

import com.example.furniture.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client dengan OkHttp Interceptor untuk header RapidAPI.
 */
public class RetrofitClient {

    private static Retrofit retrofit = null;

    /**
     * Mengembalikan instance ApiService yang siap digunakan.
     * Singleton: hanya dibuat satu kali.
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(buildOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /**
     * Membuat OkHttpClient dengan:
     * - RapidAPI header interceptor (x-rapidapi-key, x-rapidapi-host)
     * - Logging interceptor untuk debug
     */
    private static OkHttpClient buildOkHttpClient() {
        // Logging interceptor — tampilkan request/response di Logcat saat debug
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                // Header RapidAPI
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .addHeader("x-rapidapi-key", Constants.API_KEY)
                            .addHeader("x-rapidapi-host", Constants.API_HOST)
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                // Logging (aktif saat development)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    // Prevent instantiation
    private RetrofitClient() {}
}
