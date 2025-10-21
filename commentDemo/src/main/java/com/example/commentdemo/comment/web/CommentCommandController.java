package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.CommentCreateRequest;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.api.dto.CommentUpdateRequest;
import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentCommandController {

    private final CommentService commentService;
    private final ActorContextResolver actorContextResolver;

    @PostMapping("/subjects/id/{subject_id}/comments")
    public Mono<ResponseEntity<CommentResponse>> createComment(@PathVariable("subject_id") UUID subjectId,
                                                               @Valid @RequestBody CommentCreateRequest request,
                                                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                                               ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.create(subjectId, request, actor, idempotencyKey))
                .map(response -> ResponseEntity.created(buildLocation(response))
                        .eTag(generateEtag(response))
                        .body(response));
    }

    @PatchMapping("/comments/{comment_id}")
    public Mono<ResponseEntity<CommentResponse>> updateComment(@PathVariable("comment_id") Long commentId,
                                                               @Valid @RequestBody CommentUpdateRequest request,
                                                               @RequestHeader(name = "If-Match", required = false) String ifMatch,
                                                               ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.update(commentId, request, actor, ifMatch))
                .map(response -> ResponseEntity.ok()
                        .eTag(generateEtag(response))
                        .body(response));
    }

    @DeleteMapping("/comments/{comment_id}")
    public Mono<ResponseEntity<Void>> deleteComment(@PathVariable("comment_id") Long commentId,
                                                    @RequestParam(name = "soft", defaultValue = "true") boolean soft,
                                                    ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.delete(commentId, soft, actor))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    private URI buildLocation(CommentResponse response) {
        return URI.create("/api/comments/v1/comments/" + response.id());
    }

    private String generateEtag(CommentResponse response) {
        OffsetDateTime baseline = response.editedAt();
        if (baseline == null) {
            baseline = response.deletedAt();
        }
        if (baseline == null) {
            baseline = response.createdAt();
        }
        long version = baseline != null ? baseline.toInstant().toEpochMilli() : 0L;
        String token = response.id() + ":" + version;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes());
        return "W/\"" + encoded + "\"";
    }
}
