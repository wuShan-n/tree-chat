package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse;
import com.example.commentdemo.comment.api.dto.ReactionToggleRequest;
import com.example.commentdemo.comment.model.ReactionType;
import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final ActorContextResolver actorContextResolver;

    @PutMapping("/comments/{commentId}/reactions/{type}")
    public Mono<ReactionSummaryResponse> toggleReaction(@PathVariable("commentId") Long commentId,
                                                        @PathVariable("type") ReactionType type,
                                                        @Valid @RequestBody ReactionToggleRequest request,
                                                        ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> reactionService.toggleReaction(commentId, type, request, actor));
    }
}
