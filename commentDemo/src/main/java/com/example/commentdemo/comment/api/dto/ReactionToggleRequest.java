package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for toggling a reaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionToggleRequest {

    @JsonProperty("active")
    @NotNull
    private Boolean active;

    @JsonProperty("emoji_code")
    private String emojiCode;
}
