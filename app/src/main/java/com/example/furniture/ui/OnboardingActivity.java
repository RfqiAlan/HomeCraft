package com.example.furniture.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.furniture.R;
import com.example.furniture.adapter.OnboardingAdapter;
import com.example.furniture.model.OnboardingItem;
import com.example.furniture.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutIndicators;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnNextFab;
    private TextView btnSkip;
    private MaterialButton btnGetStarted;
    private ViewPager2 onboardingViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnNextFab = findViewById(R.id.btnNextFab);
        btnSkip = findViewById(R.id.btnSkip);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        onboardingViewPager = findViewById(R.id.onboardingViewPager);

        setupOnboardingItems();
        onboardingViewPager.setAdapter(onboardingAdapter);

        setupIndicators();
        setCurrentIndicator(0);

        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNextFab.hide();
                    btnSkip.animate().alpha(0f).setDuration(200).withEndAction(() -> btnSkip.setVisibility(View.INVISIBLE)).start();
                    layoutIndicators.animate().alpha(0f).setDuration(200).withEndAction(() -> layoutIndicators.setVisibility(View.INVISIBLE)).start();
                    
                    btnGetStarted.setAlpha(0f);
                    btnGetStarted.setVisibility(View.VISIBLE);
                    btnGetStarted.animate().alpha(1f).setDuration(300).start();
                } else {
                    btnNextFab.show();
                    btnSkip.setVisibility(View.VISIBLE);
                    btnSkip.animate().alpha(1f).setDuration(200).start();
                    layoutIndicators.setVisibility(View.VISIBLE);
                    layoutIndicators.animate().alpha(1f).setDuration(200).start();
                    
                    btnGetStarted.animate().alpha(0f).setDuration(200).withEndAction(() -> btnGetStarted.setVisibility(View.INVISIBLE)).start();
                }
            }
        });

        btnNextFab.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            }
        });

        btnSkip.setOnClickListener(v -> navigateToMain());
        btnGetStarted.setOnClickListener(v -> navigateToMain());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        // Using open source free lottie animation URLs
        OnboardingItem itemFastDelivery = new OnboardingItem(
                "https://lottie.host/801a6176-a079-45ad-bb02-f8db89d5bc56/xK246Y0N1a.json",
                "Temukan Gaya Anda",
                "Jelajahi ribuan furnitur premium\nuntuk mempercantik rumah."
        );

        OnboardingItem itemQuality = new OnboardingItem(
                "https://lottie.host/88e0b6ab-e83c-44af-8898-75c1dd87c5eb/4h0x3B6t9O.json",
                "Kualitas Terbaik",
                "Setiap produk melalui kontrol kualitas ketat\ndemi kenyamanan Anda."
        );

        OnboardingItem itemPremium = new OnboardingItem(
                "https://lottie.host/43e7fb78-f737-4d08-a53e-2bbaea906666/o91R8G0Mhh.json",
                "Pengiriman Cepat",
                "Sampai di depan pintu Anda\ndengan aman dan cepat."
        );

        onboardingItems.add(itemFastDelivery);
        onboardingItems.add(itemQuality);
        onboardingItems.add(itemPremium);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.bg_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_indicator_active)
                );
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.bg_indicator_inactive)
                );
            }
        }
    }

    private void navigateToMain() {
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.setFirstTimeLaunch(false);

        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
