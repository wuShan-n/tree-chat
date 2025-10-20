package com.example.commentdemo.comment.api.dto;

import com.example.commentdemo.comment.model.CommentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for editing an existing comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequest {

    @JsonProperty("body_md")
    @Size(max = 20000)
    private String bodyMd;

    @JsonProperty("body_html")
    private String bodyHtml;

    @JsonProperty("status")
    private CommentStatus status;

    @JsonProperty("hard_delete")
    private Boolean hardDelete;
}
