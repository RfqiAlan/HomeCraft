package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response untuk endpoint {@code auto-complete} (Deprecating).
 * Struktur JSON: {@code { "Suggestions": [ ["keyword", "", ""], ... ] }}
 * Tiap suggestion adalah array 3 elemen; elemen pertama = keyword.
 */
public class AutoCompleteResponse {

    @SerializedName("Suggestions")
    private List<List<String>> suggestions;

    public List<List<String>> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<List<String>> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Helper: ambil hanya string keyword dari setiap suggestion.
     */
    public List<String> getKeywords() {
        List<String> keywords = new ArrayList<>();
        if (suggestions == null) return keywords;
        for (List<String> s : suggestions) {
            if (s != null && !s.isEmpty() && s.get(0) != null && !s.get(0).isEmpty()) {
                keywords.add(s.get(0));
            }
        }
        return keywords;
    }
}
