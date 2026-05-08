package com.example.furniture.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furniture.R;
import com.example.furniture.model.Review;

import java.util.List;

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
        
        holder.tvReviewerName.setText(review.getUserNickname() != null ? review.getUserNickname() : "Anonymous");
        // Submission time might be a full ISO date, for simplicity just set the string, or parse it to a nicer format.
        String dateStr = review.getSubmissionTime() != null ? review.getSubmissionTime() : "";
        holder.tvReviewDate.setText(dateStr);
        holder.tvReviewText.setText(review.getReviewText());
        
        // Simple representation of rating using the stars container. For a real app, you'd show/hide or tint based on the value.
        // Assuming 5 stars static layout for now.
    }

    @Override
    public int getItemCount() {
        return reviewList == null ? 0 : reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerName;
        TextView tvReviewDate;
        TextView tvReviewText;
        ImageView ivReviewerProfile;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewText = itemView.findViewById(R.id.tvReviewText);
            ivReviewerProfile = itemView.findViewById(R.id.ivReviewerProfile);
        }
    }
}
