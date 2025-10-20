package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response view of a subject returned to API consumers.
 */
@Builder
public record SubjectResponse(
        @JsonProperty("subject_id") UUID subjectId,
        @JsonProperty("subject_key") String subjectKey,
        @JsonProperty("is_locked") boolean locked,
        @JsonProperty("is_archived") boolean archived,
        @JsonProperty("policy") JsonNode policy,
        @JsonProperty("comment_count") int commentCount,
        @JsonProperty("visible_count") int visibleCount,
        @JsonProperty("last_commented_at") OffsetDateTime lastCommentedAt,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
}
