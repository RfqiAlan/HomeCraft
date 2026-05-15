package com.example.furniture.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.ReviewAdapter;
import com.example.furniture.api.ApiService;
import com.example.furniture.api.RetrofitClient;
import com.example.furniture.model.Review;
import com.example.furniture.model.ReviewResponse;
import com.example.furniture.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ReviewActivity — menampilkan daftar review produk dari API Kohls.
 * Menerima EXTRA_PRODUCT_ID dari DetailActivity.
 */
public class ReviewActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvRating;
    private TextView tvReviewCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // ─── Ambil productId dari Intent ─────────────────────────────────────────
        String productId = null;
        if (getIntent() != null) {
            productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        }

        // ─── Init Views ───────────────────────────────────────────────────────────
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        progressBar    = findViewById(R.id.progressBarReview);
        tvEmpty        = findViewById(R.id.tvEmptyReview);
        tvRating       = findViewById(R.id.tvRating);
        tvReviewCount  = findViewById(R.id.tvReviewCount);

        rvReviews = findViewById(R.id.rvReviews);
        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            reviewAdapter = new ReviewAdapter(new ArrayList<>());
            rvReviews.setAdapter(reviewAdapter);
        }

        View btnWriteReview = findViewById(R.id.btnWriteReview);
        if (btnWriteReview != null) {
            btnWriteReview.setOnClickListener(v ->
                    Toast.makeText(this, "Fitur tulis review segera hadir!", Toast.LENGTH_SHORT).show());
        }

        // ─── Load Data ─────────────────────────────────────────────────────────────
        if (productId != null && !productId.isEmpty()) {
            loadReviews(productId);
        } else {
            showEmpty("ID produk tidak ditemukan.");
        }
    }

    // ─── API ─────────────────────────────────────────────────────────────────────

    private void loadReviews(String productId) {
        showLoading();
        ApiService apiService = RetrofitClient.getApiService();
        Call<ReviewResponse> call = apiService.getReviews(
                productId,
                Constants.DEFAULT_REVIEW_LIMIT,
                0,
                Constants.DEFAULT_REVIEW_SORT
        );

        call.enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call,
                                   @NonNull Response<ReviewResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    ReviewResponse reviewResponse = response.body();
                    List<Review> reviews = reviewResponse.getReviews();
                    int total = reviewResponse.getTotalResults();

                    if (reviews != null && !reviews.isEmpty()) {
                        reviewAdapter.setReviewList(reviews);
                        updateSummary(reviews, total);
                        showContent();
                    } else {
                        showEmpty("Belum ada review untuk produk ini.");
                    }
                } else {
                    showEmpty("Gagal memuat review. Coba lagi nanti.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                hideLoading();
                showEmpty("Tidak dapat terhubung ke server.");
            }
        });
    }

    // ─── Helper: Hitung rata-rata rating dan update header ───────────────────────

    private void updateSummary(List<Review> reviews, int total) {
        if (tvReviewCount != null) {
            tvReviewCount.setText(total + " reviews");
        }
        if (tvRating != null && !reviews.isEmpty()) {
            double avg = 0;
            for (Review r : reviews) avg += r.getRating();
            avg /= reviews.size();
            tvRating.setText(String.format("%.1f", avg));
        }
    }

    // ─── UI State ─────────────────────────────────────────────────────────────────

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (rvReviews   != null) rvReviews.setVisibility(View.GONE);
        if (tvEmpty     != null) tvEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showContent() {
        if (rvReviews != null) rvReviews.setVisibility(View.VISIBLE);
        if (tvEmpty   != null) tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String msg) {
        if (rvReviews != null) rvReviews.setVisibility(View.GONE);
        if (tvEmpty   != null) {
            tvEmpty.setText(msg);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }
}
