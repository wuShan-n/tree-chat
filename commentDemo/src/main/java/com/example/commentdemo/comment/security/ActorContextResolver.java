package com.example.commentdemo.comment.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts basic actor information from the incoming request.
 * <p>
 * This placeholder version reads {@code X-Actor-Id}, {@code X-Actor-Urn} and
 * {@code X-Actor-Roles} headers to build the context. Replace with JWT introspection later.
 */
@Component
@RequiredArgsConstructor
public class ActorContextResolver {

    private static final String HEADER_ACTOR_ID = "X-Actor-Id";
    private static final String HEADER_ACTOR_URN = "X-Actor-Urn";
    private static final String HEADER_ACTOR_ROLES = "X-Actor-Roles";

    public Mono<ActorContext> resolve(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        var actorId = parseLong(request.getHeaders().getFirst(HEADER_ACTOR_ID));
        var actorUrn = request.getHeaders().getFirst(HEADER_ACTOR_URN);
        var rolesHeader = request.getHeaders().getFirst(HEADER_ACTOR_ROLES);
        var roles = StringUtils.hasText(rolesHeader)
                ? Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet())
                : Set.<String>of();

        return Mono.just(
                ActorContext.builder()
                        .actorId(actorId)
                        .actorUrn(actorUrn)
                        .roles(roles)
                        .build()
        );
    }

    private Long parseLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
