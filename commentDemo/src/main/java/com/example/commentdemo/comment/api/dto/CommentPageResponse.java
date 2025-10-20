package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Envelope with pagination cursor.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentPageResponse(
        @JsonProperty("items") List<CommentResponse> items,
        @JsonProperty("next_cursor") String nextCursor
) {
}
