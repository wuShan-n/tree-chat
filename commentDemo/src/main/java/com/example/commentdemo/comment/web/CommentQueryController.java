package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.CommentPageResponse;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.CommentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Validated
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentQueryController {

    private final CommentService commentService;
    private final ActorContextResolver actorContextResolver;

    @GetMapping("/subjects/{subjectId}/comments")
    public Mono<CommentPageResponse> listTopLevel(@PathVariable("subjectId") UUID subjectId,
                                                  @RequestParam(name = "view", defaultValue = "new") String view,
                                                  @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
                                                  @RequestParam(name = "cursor", required = false) String cursor,
                                                  ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.listTopLevel(subjectId, view, limit, cursor, actor));
    }

    @GetMapping("/comments/{commentId}")
    public Mono<CommentResponse> getComment(@PathVariable("commentId") Long commentId,
                                            ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.getComment(commentId, actor));
    }

    @GetMapping("/comments/{commentId}/replies")
    public Mono<CommentPageResponse> listReplies(@PathVariable("commentId") Long commentId,
                                                 @RequestParam(name = "order", defaultValue = "structure") String order,
                                                 @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(200) int limit,
                                                 @RequestParam(name = "cursor", required = false) String cursor,
                                                 ServerWebExchange exchange) {
        return actorContextResolver.resolve(exchange)
                .flatMap(actor -> commentService.listReplies(commentId, order, limit, cursor, actor));
    }
}
