package com.example.commentdemo.comment.repository;

import com.example.commentdemo.comment.domain.entity.CommentReactionEntity;
import com.example.commentdemo.comment.model.ReactionType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface CommentReactionRepository extends ReactiveCrudRepository<CommentReactionEntity, Long> {

    Mono<CommentReactionEntity> findByCommentIdAndActorIdAndTypeAndEmojiCode(Long commentId, Long actorId, ReactionType type, String emojiCode);

    Flux<CommentReactionEntity> findByCommentId(Long commentId);

    Flux<CommentReactionEntity> findByCommentIdInAndActorId(Collection<Long> commentIds, Long actorId);
}
