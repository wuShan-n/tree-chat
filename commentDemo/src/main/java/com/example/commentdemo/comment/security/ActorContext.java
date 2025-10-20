package com.example.commentdemo.comment.security;

import lombok.Builder;

import java.util.Collections;
import java.util.Set;

/**
 * Simplified representation of the caller extracted from headers or JWT.
 */
@Builder(toBuilder = true)
public record ActorContext(
        Long actorId,
        String actorUrn,
        Set<String> roles
) {

    public static ActorContext anonymous() {
        return ActorContext.builder()
                .roles(Collections.emptySet())
                .build();
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
