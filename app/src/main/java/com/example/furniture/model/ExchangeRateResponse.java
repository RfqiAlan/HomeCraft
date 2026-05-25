package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Model respons dari https://api.exchangerate-api.com/v4/latest/USD
 * Contoh JSON: { "base": "USD", "rates": { "IDR": 16234.5, ... } }
 */
public class ExchangeRateResponse {

    @SerializedName("base")
    private String base;

    @SerializedName("rates")
    private Map<String, Double> rates;

    public String getBase() { return base; }

    public Map<String, Double> getRates() { return rates; }

    /** Ambil kurs IDR langsung, return fallback jika tidak ada. */
    public double getIdrRate(double fallback) {
        if (rates == null) return fallback;
        Double idr = rates.get("IDR");
        return (idr != null && idr > 0) ? idr : fallback;
    }
}
