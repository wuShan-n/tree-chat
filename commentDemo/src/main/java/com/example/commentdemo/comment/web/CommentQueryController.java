package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.CommentPageResponse;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.CommentService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@RestController
@Validated
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentQueryController {

    private final CommentService commentService;
    private final ActorContextResolver actorContextResolver;

    @GetMapping("/subjects/id/{subject_id}/comments")
    public Mono<CommentPageResponse> listTopLevel(@PathVariable("subject_id") UUID subjectId,
                                                  @RequestParam(name = "view", defaultValue = "best") String view,
                                                  @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
                                                  @RequestParam(name = "cursor", required = false) String cursor,
                                                  @RequestParam(name = "status", defaultValue = "published") String status,
                                                  @RequestParam(name = "with_counts", defaultValue = "true") boolean withCounts,
                                                  @RequestParam(name = "with_my_reaction", defaultValue = "true") boolean withMyReaction,
                                                  ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.listTopLevel(subjectId, view, limit, cursor, status, withCounts, withMyReaction, actor));
    }

    @GetMapping("/comments/{comment_id}")
    public Mono<ResponseEntity<CommentResponse>> getComment(@PathVariable("comment_id") Long commentId,
                                                            ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.getComment(commentId, actor))
                .map(this::toOkResponse);
    }

    @GetMapping("/comments/{comment_id}/replies")
    public Mono<CommentPageResponse> listReplies(@PathVariable("comment_id") Long commentId,
                                                 @RequestParam(name = "order", defaultValue = "structure") String order,
                                                 @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(200) int limit,
                                                 @RequestParam(name = "cursor", required = false) String cursor,
                                                 @RequestParam(name = "collapse_below", defaultValue = "0") @DecimalMin("0.0") @DecimalMax("1.0") double collapseBelow,
                                                 @RequestParam(name = "with_counts", defaultValue = "true") boolean withCounts,
                                                 @RequestParam(name = "with_my_reaction", defaultValue = "true") boolean withMyReaction,
                                                 ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.listReplies(commentId, order, limit, cursor, collapseBelow, withCounts, withMyReaction, actor));
    }

    private ResponseEntity<CommentResponse> toOkResponse(CommentResponse response) {
        return ResponseEntity.ok()
                .eTag(generateEtag(response))
                .body(response);
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
