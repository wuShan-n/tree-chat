package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update payload for subject flags and policy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPatchRequest {

    @JsonProperty("is_locked")
    private Boolean locked;

    @JsonProperty("is_archived")
    private Boolean archived;

    @JsonProperty("policy")
    private JsonNode policy;
}
