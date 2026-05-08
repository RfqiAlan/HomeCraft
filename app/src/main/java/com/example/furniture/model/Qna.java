package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

/**
 * Satu item Q&amp;A produk (endpoint qnas/list).
 * Mapping field mengikuti schema Bazaarvoice yang dipakai Kohls.
 */
public class Qna {

    @SerializedName("Id")
    private String id;

    @SerializedName("ProductId")
    private String productId;

    @SerializedName("UserNickname")
    private String userNickname;

    @SerializedName("UserLocation")
    private String userLocation;

    @SerializedName("QuestionSummary")
    private String questionSummary;

    @SerializedName("QuestionDetails")
    private String questionDetails;

    @SerializedName("TotalAnswerCount")
    private int totalAnswerCount;

    @SerializedName("SubmissionTime")
    private String submissionTime;

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

    public String getUserLocation() { return userLocation; }
    public void setUserLocation(String userLocation) { this.userLocation = userLocation; }

    public String getQuestionSummary() { return questionSummary; }
    public void setQuestionSummary(String q) { this.questionSummary = q; }

    public String getQuestionDetails() { return questionDetails; }
    public void setQuestionDetails(String q) { this.questionDetails = q; }

    public int getTotalAnswerCount() { return totalAnswerCount; }
    public void setTotalAnswerCount(int v) { this.totalAnswerCount = v; }

    public String getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(String v) { this.submissionTime = v; }
}
