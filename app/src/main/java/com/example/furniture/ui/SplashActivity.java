package com.example.furniture.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.utils.ExchangeRateManager;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.SessionManager;
import com.example.furniture.utils.ThemeManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply locale before view is inflated
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Fetch latest exchange rate (fire and forget)
        ExchangeRateManager.fetchRate(this, null);

        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);

        // Animasikan Logo (Fade in + Overshoot Scale + Rotation)
        ivLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(1200)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // Animasikan Teks (Fade in + Slide up)
        tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(500) // Mulai setelah logo muncul
                .setInterpolator(new DecelerateInterpolator())
                .start();
                
        // Animasikan Tagline
        tvTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Pindah ke MainActivity setelah animasi selesai
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, 2800);
    }

    private void navigateToMain() {
        if (isFinishing()) return;

        SessionManager sessionManager = new SessionManager(this);
        Intent intent;

        if (sessionManager.isFirstTimeLaunch()) {
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
