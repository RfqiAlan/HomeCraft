package com.example.furniture.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.adapter.ReviewAdapter;
import com.example.furniture.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        
        // Set up dummy data
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
        
        findViewById(R.id.btnWriteReview).setOnClickListener(v -> {
            Toast.makeText(this, "Write a review clicked", Toast.LENGTH_SHORT).show();
        });
    }
}
