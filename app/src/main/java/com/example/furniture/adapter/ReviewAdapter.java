package com.example.furniture.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.model.Review;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    public void setReviewList(List<Review> reviewList) {
        this.reviewList = reviewList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // ─── Nama ────────────────────────────────────────────────────────────────
        String name = review.getUserNickname() != null ? review.getUserNickname() : "Anonymous";
        holder.tvReviewerName.setText(name);

        // ─── Avatar inisial ──────────────────────────────────────────────────────
        String initial = name.length() > 0 ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
        holder.tvAvatarInitial.setText(initial);

        // ─── Lokasi ──────────────────────────────────────────────────────────────
        if (review.getUserLocation() != null && !review.getUserLocation().isEmpty()) {
            holder.tvReviewerLocation.setText(review.getUserLocation());
            holder.tvReviewerLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewerLocation.setVisibility(View.GONE);
        }

        // ─── Tanggal (format ulang dari ISO 8601) ─────────────────────────────────
        String dateStr = formatDate(review.getSubmissionTime());
        holder.tvReviewDate.setText(dateStr);

        // ─── Rating bintang dinamis ───────────────────────────────────────────────
        holder.ratingBar.setRating(review.getRating());

        // ─── Judul review ────────────────────────────────────────────────────────
        if (review.getTitle() != null && !review.getTitle().isEmpty()) {
            holder.tvReviewTitle.setText(review.getTitle());
            holder.tvReviewTitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewTitle.setVisibility(View.GONE);
        }

        // ─── Isi review ──────────────────────────────────────────────────────────
        if (review.getReviewText() != null && !review.getReviewText().isEmpty()) {
            holder.tvReviewText.setText(review.getReviewText());
            holder.tvReviewText.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewText.setVisibility(View.GONE);
        }

        // ─── Badge rekomendasi ───────────────────────────────────────────────────
        if (Boolean.TRUE.equals(review.getRecommended())) {
            holder.tvRecommended.setVisibility(View.VISIBLE);
        } else {
            holder.tvRecommended.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList == null ? 0 : reviewList.size();
    }

    /**
     * Format tanggal dari ISO 8601 ("2023-10-15T12:00:00.000+00:00") → "15 Oct 2023"
     */
    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            // API Kohls: "2023-10-15T12:00:00.000+00:00"
            SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            Date date = inputFmt.parse(rawDate);
            if (date != null) {
                SimpleDateFormat outputFmt = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                return outputFmt.format(date);
            }
        } catch (ParseException e) {
            // Fallback: ambil 10 karakter pertama (YYYY-MM-DD)
            if (rawDate.length() >= 10) return rawDate.substring(0, 10);
        }
        return rawDate;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitial;
        TextView tvReviewerName;
        TextView tvReviewerLocation;
        TextView tvReviewDate;
        RatingBar ratingBar;
        TextView tvReviewTitle;
        TextView tvReviewText;
        TextView tvRecommended;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial      = itemView.findViewById(R.id.tvAvatarInitial);
            tvReviewerName       = itemView.findViewById(R.id.tvReviewerName);
            tvReviewerLocation   = itemView.findViewById(R.id.tvReviewerLocation);
            tvReviewDate         = itemView.findViewById(R.id.tvReviewDate);
            ratingBar            = itemView.findViewById(R.id.ratingBar);
            tvReviewTitle        = itemView.findViewById(R.id.tvReviewTitle);
            tvReviewText         = itemView.findViewById(R.id.tvReviewText);
            tvRecommended        = itemView.findViewById(R.id.tvRecommended);
        }
    }
}
