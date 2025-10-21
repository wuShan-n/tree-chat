package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Aggregated metrics view for a subject.
 */
public record SubjectMetricsResponse(
        @JsonProperty("comment_count") int commentCount,
        @JsonProperty("visible_count") int visibleCount,
        @JsonProperty("top_contributors") List<TopContributor> topContributors,
        @JsonProperty("p95_latency_ms") int p95LatencyMs
) {

    public SubjectMetricsResponse {
        topContributors = topContributors == null ? List.of() : List.copyOf(topContributors);
    }

    /**
     * Lightweight contributor summary for the leaderboard.
     */
    public record TopContributor(
            @JsonProperty("actor_id") long actorId,
            @JsonProperty("count") int count
    ) {
    }
}
