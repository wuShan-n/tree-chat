package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.CommentCreateRequest;
import com.example.commentdemo.comment.api.dto.CommentPageResponse;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.api.dto.CommentUpdateRequest;
import com.example.commentdemo.comment.security.ActorContext;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CommentService {

    Mono<CommentPageResponse> listTopLevel(UUID subjectId, String view, int limit, String cursor, ActorContext actor);

    Mono<CommentPageResponse> listReplies(Long commentId, String order, int limit, String cursor, ActorContext actor);

    Mono<CommentResponse> getComment(Long commentId, ActorContext actor);

    Mono<CommentResponse> create(UUID subjectId, CommentCreateRequest request, ActorContext actor, String idempotencyKey);

    Mono<CommentResponse> update(Long commentId, CommentUpdateRequest request, ActorContext actor, String ifMatch);
}
