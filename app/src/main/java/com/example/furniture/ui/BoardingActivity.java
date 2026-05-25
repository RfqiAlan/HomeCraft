package com.example.furniture.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.furniture.R;
import com.example.furniture.utils.ExchangeRateManager;
import com.example.furniture.utils.LanguageManager;
import com.example.furniture.utils.ThemeManager;

public class BoardingActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Terapkan locale sebelum view di-inflate
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding);

        // Fetch kurs terbaru saat app start (fire and forget)
        ExchangeRateManager.fetchRate(this, null);

        Button btnGetStarted = findViewById(R.id.btn_get_started);
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(BoardingActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
