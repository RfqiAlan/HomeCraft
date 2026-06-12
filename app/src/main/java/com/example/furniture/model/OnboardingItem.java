package com.example.furniture.model;

public class OnboardingItem {
    private String lottieUrl;
    private String title;
    private String description;

    public OnboardingItem(String lottieUrl, String title, String description) {
        this.lottieUrl = lottieUrl;
        this.title = title;
        this.description = description;
    }

    public String getLottieUrl() {
        return lottieUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
