package com.example.commentdemo.comment.api.dto;

import com.example.commentdemo.comment.model.CommentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/**
 * Comment response projection aligned with the OpenAPI contract.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponse(
        @JsonProperty("id") long id,
        @JsonProperty("subject_id") UUID subjectId,
        @JsonProperty("root_id") Long rootId,
        @JsonProperty("parent_id") Long parentId,
        @JsonProperty("depth") int depth,
        @JsonProperty("author") Author author,
        @JsonProperty("body_md") String bodyMd,
        @JsonProperty("body_html") String bodyHtml,
        @JsonProperty("status") CommentStatus status,
        @JsonProperty("toxicity_score") BigDecimal toxicityScore,
        @JsonProperty("counters") Counters counters,
        @JsonProperty("my_reaction") MyReaction myReaction,
        @JsonProperty("quality") Quality quality,
        @JsonProperty("collapsed") Collapse collapsed,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("edited_at") OffsetDateTime editedAt,
        @JsonProperty("deleted_at") OffsetDateTime deletedAt,
        @JsonProperty("extensions") Map<String, Object> extensions
) {

    @Builder
    public record Author(
            @JsonProperty("id") Long id,
            @JsonProperty("urn") String urn
    ) {
    }

    @Builder
    public record Counters(
            @JsonProperty("up") int up,
            @JsonProperty("down") int down,
            @JsonProperty("replies") int replies
    ) {
    }

    @Builder
    public record MyReaction(
            @JsonProperty("up") boolean up,
            @JsonProperty("down") boolean down,
            @JsonProperty("emoji") Set<String> emoji
    ) {
    }

    @Builder
    public record Quality(
            @JsonProperty("best_score") BigDecimal bestScore,
            @JsonProperty("hot_score") BigDecimal hotScore
    ) {
    }

    @Builder
    public record Collapse(
            @JsonProperty("value") boolean value,
            @JsonProperty("reason") String reason
    ) {
    }
}
