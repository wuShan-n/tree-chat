package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for subject registration/update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectUpsertRequest {

    @JsonProperty("policy")
    private JsonNode policy;

    @JsonProperty("is_locked")
    private Boolean locked;

    @JsonProperty("is_archived")
    private Boolean archived;

}
