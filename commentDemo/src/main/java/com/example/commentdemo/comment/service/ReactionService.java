package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse;
import com.example.commentdemo.comment.api.dto.ReactionToggleRequest;
import com.example.commentdemo.comment.model.ReactionType;
import com.example.commentdemo.comment.security.ActorContext;
import reactor.core.publisher.Mono;

public interface ReactionService {

    Mono<ReactionSummaryResponse> toggleReaction(Long commentId, ReactionType type, ReactionToggleRequest request, ActorContext actor);

    Mono<ReactionSummaryResponse> getReactions(Long commentId, ActorContext actor);
}
