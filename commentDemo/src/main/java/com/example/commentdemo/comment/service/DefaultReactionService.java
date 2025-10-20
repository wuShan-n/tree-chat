package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse;
import com.example.commentdemo.comment.api.dto.ReactionToggleRequest;
import com.example.commentdemo.comment.model.ReactionType;
import com.example.commentdemo.comment.security.ActorContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DefaultReactionService implements ReactionService {

    @Override
    public Mono<ReactionSummaryResponse> toggleReaction(Long commentId, ReactionType type, ReactionToggleRequest request, ActorContext actor) {
        return Mono.error(new UnsupportedOperationException("Reaction operations are not implemented yet"));
    }
}
