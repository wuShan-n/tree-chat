package com.example.commentdemo.comment.api.dto;

import com.example.commentdemo.comment.model.CommentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    @JsonProperty("parent_id")
    private Long parentId;

    @JsonProperty("body_md")
    @NotBlank
    @Size(max = 20000)
    private String bodyMd;

    @JsonProperty("body_html")
    private String bodyHtml;

    @JsonProperty("status")
    private CommentStatus status;
}
