package com.example.furniture.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

/**
 * Utility class untuk mengecek koneksi internet.
 * Digunakan oleh HomeFragment, CategoryFragment, dan DetailActivity.
 */
public class NetworkUtils {

    /**
     * Mengecek apakah perangkat terhubung ke internet.
     *
     * @param context Context aplikasi
     * @return true jika internet tersedia, false jika tidak
     */
    public static boolean isInternetAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);

            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            // Fallback untuk API < 23
            android.net.NetworkInfo activeNetwork =
                    connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    // Prevent instantiation
    private NetworkUtils() {}
}
