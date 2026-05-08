package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

/**
 * Satu item review produk (endpoint reviews/list).
 * Hanya memetakan field yang relevan untuk UI.
 */
public class Review {

    @SerializedName("Id")
    private String id;

    @SerializedName("UserNickname")
    private String userNickname;

    @SerializedName("UserLocation")
    private String userLocation;

    @SerializedName("Rating")
    private int rating;

    @SerializedName("Title")
    private String title;

    @SerializedName("ReviewText")
    private String reviewText;

    @SerializedName("SubmissionTime")
    private String submissionTime;

    @SerializedName("IsRecommended")
    private Boolean isRecommended;

    @SerializedName("TotalPositiveFeedbackCount")
    private int totalPositiveFeedbackCount;

    @SerializedName("TotalNegativeFeedbackCount")
    private int totalNegativeFeedbackCount;

    @SerializedName("ProductId")
    private String productId;

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

    public String getUserLocation() { return userLocation; }
    public void setUserLocation(String userLocation) { this.userLocation = userLocation; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(String submissionTime) { this.submissionTime = submissionTime; }

    public Boolean getRecommended() { return isRecommended; }
    public void setRecommended(Boolean recommended) { isRecommended = recommended; }

    public int getTotalPositiveFeedbackCount() { return totalPositiveFeedbackCount; }
    public void setTotalPositiveFeedbackCount(int v) { this.totalPositiveFeedbackCount = v; }

    public int getTotalNegativeFeedbackCount() { return totalNegativeFeedbackCount; }
    public void setTotalNegativeFeedbackCount(int v) { this.totalNegativeFeedbackCount = v; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
}
