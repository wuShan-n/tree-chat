package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.CommentCreateRequest;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.api.dto.CommentUpdateRequest;
import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentCommandController {

    private final CommentService commentService;
    private final ActorContextResolver actorContextResolver;

    @PostMapping("/subjects/{subjectId}/comments")
    public Mono<CommentResponse> createComment(@PathVariable("subjectId") UUID subjectId,
                                               @Valid @RequestBody CommentCreateRequest request,
                                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                               ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.create(subjectId, request, actor, idempotencyKey));
    }

    @PatchMapping("/comments/{commentId}")
    public Mono<CommentResponse> updateComment(@PathVariable("commentId") Long commentId,
                                               @Valid @RequestBody CommentUpdateRequest request,
                                               @RequestHeader(name = "If-Match", required = false) String ifMatch,
                                               ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.update(commentId, request, actor, ifMatch));
    }
}
