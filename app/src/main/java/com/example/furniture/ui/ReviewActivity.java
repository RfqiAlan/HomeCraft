package com.example.furniture.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.ReviewAdapter;
import com.example.furniture.model.Review;
import com.example.furniture.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // Ambil productId dari intent (opsional, bisa digunakan untuk load API review)
        String productId = null;
        if (getIntent() != null) {
            productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        }

        // Tombol kembali — null-safe
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));

            // Data dummy review
            List<Review> reviewList = new ArrayList<>();

            Review r1 = new Review();
            r1.setUserNickname("Bruno Fernandes");
            r1.setSubmissionTime("20/03/2020");
            r1.setRating(5);
            r1.setReviewText("Nice Furniture with good delivery. The delivery time is very fast. Then products look like exactly the picture in the app. Besides, color is also the same and quality is very good despite very cheap price");

            Review r2 = new Review();
            r2.setUserNickname("Tracy Mosby");
            r2.setSubmissionTime("20/03/2020");
            r2.setRating(5);
            r2.setReviewText("Nice Furniture with good delivery. The delivery time is very fast. Then products look like exactly the picture in the app. Besides, color is also the same and quality is very good despite very cheap price");

            reviewList.add(r1);
            reviewList.add(r2);

            ReviewAdapter adapter = new ReviewAdapter(reviewList);
            rvReviews.setAdapter(adapter);
        }

        // Tombol tulis review — null-safe
        android.view.View btnWrite = findViewById(R.id.btnWriteReview);
        if (btnWrite != null) {
            btnWrite.setOnClickListener(v ->
                    Toast.makeText(this, "Fitur tulis review segera hadir!", Toast.LENGTH_SHORT).show());
        }
    }
}
