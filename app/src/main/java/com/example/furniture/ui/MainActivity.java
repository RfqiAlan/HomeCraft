package com.example.furniture.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.furniture.R;
import com.example.furniture.utils.ExchangeRateManager;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity — launcher activity, host untuk semua Fragment.
 * Mengatur Bottom Navigation dan Navigation Component.
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch kurs terbaru di background (update cache jika online)
        ExchangeRateManager.fetchRate(this, null);

        setupNavigation();
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    /**
     * Parsing URL deep link dan pindah ke halaman yang sesuai.
     */
    private void handleDeepLink(android.content.Intent intent) {
        if (intent != null && android.content.Intent.ACTION_VIEW.equals(intent.getAction())) {
            android.net.Uri data = intent.getData();
            if (data != null && "homecraft.com".equals(data.getHost())) {
                String path = data.getPath();
                if (path != null) {
                    if (path.startsWith("/product/")) {
                        // Extract product ID: /product/123 -> 123
                        String productId = path.substring("/product/".length());
                        if (!productId.isEmpty()) {
                            android.content.Intent detailIntent = new android.content.Intent(this, DetailActivity.class);
                            detailIntent.putExtra(com.example.furniture.utils.Constants.EXTRA_PRODUCT_ID, productId);
                            startActivity(detailIntent);
                        }
                    } else if (path.equals("/cart")) {
                        if (navController != null) {
                            navController.navigate(R.id.nav_cart);
                        }
                    }
                }
            }
        }
    }

    /**
     * Menghubungkan NavController dengan BottomNavigationView.
     */
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null && navController != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }
}
