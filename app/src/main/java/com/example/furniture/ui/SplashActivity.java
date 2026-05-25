package com.example.furniture.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.utils.ExchangeRateManager;
import com.example.furniture.utils.LanguageManager;
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

        VideoView videoView = findViewById(R.id.videoView);
        TextView tvSkip = findViewById(R.id.tvSkip);

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.splash;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        videoView.setOnCompletionListener(mp -> navigateToMain());

        tvSkip.setOnClickListener(v -> navigateToMain());

        videoView.start();
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
