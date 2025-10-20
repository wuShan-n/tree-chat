package com.example.commentdemo.comment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReactionSummaryResponse(
        @JsonProperty("summary") Summary summary,
        @JsonProperty("my_reaction") MyReaction myReaction
) {

    @Builder
    public record Summary(
            @JsonProperty("up") int up,
            @JsonProperty("down") int down,
            @JsonProperty("emoji") int emoji
    ) {
    }

    @Builder
    public record MyReaction(
            @JsonProperty("up") boolean up,
            @JsonProperty("down") boolean down
    ) {
    }
}
