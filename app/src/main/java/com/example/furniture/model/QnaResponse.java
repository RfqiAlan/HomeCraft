package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response wrapper untuk endpoint {@code qnas/list}.
 * Struktur sama dengan reviews/list: payload.Results[].
 */
public class QnaResponse {

    @SerializedName("payload")
    private Payload payload;

    public Payload getPayload() { return payload; }
    public void setPayload(Payload payload) { this.payload = payload; }

    public List<Qna> getQnas() {
        return payload != null ? payload.getResults() : null;
    }

    public int getTotalResults() {
        return payload != null ? payload.getTotalResults() : 0;
    }

    public static class Payload {

        @SerializedName("Results")
        private List<Qna> results;

        @SerializedName("TotalResults")
        private int totalResults;

        @SerializedName("Limit")
        private int limit;

        @SerializedName("Offset")
        private int offset;

        public List<Qna> getResults() { return results; }
        public void setResults(List<Qna> results) { this.results = results; }

        public int getTotalResults() { return totalResults; }
        public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
    }
}
