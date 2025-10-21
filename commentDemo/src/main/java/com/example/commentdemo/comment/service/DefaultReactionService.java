package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse;
import com.example.commentdemo.comment.api.dto.ReactionToggleRequest;
import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse.EmojiSummary;
import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse.MyReaction;
import com.example.commentdemo.comment.api.dto.ReactionSummaryResponse.Summary;
import com.example.commentdemo.comment.domain.entity.CommentReactionEntity;
import com.example.commentdemo.comment.model.ReactionType;
import com.example.commentdemo.comment.repository.CommentReactionRepository;
import com.example.commentdemo.comment.security.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultReactionService implements ReactionService {

    private final CommentReactionRepository reactionRepository;

    @Override
    public Mono<ReactionSummaryResponse> toggleReaction(Long commentId, ReactionType type, ReactionToggleRequest request, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        Assert.notNull(type, "type must not be null");
        Assert.notNull(request, "request must not be null");

        return Mono.defer(() -> {
            if (actor == null || actor.actorId() == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Actor id required for reaction updates"));
            }

            boolean activate = Boolean.TRUE.equals(request.getActive());
            String emojiCode = resolveEmojiCode(type, request);

            Mono<Void> exclusivity = Mono.empty();
            if (activate && (type == ReactionType.UP || type == ReactionType.DOWN)) {
                ReactionType opposite = type == ReactionType.UP ? ReactionType.DOWN : ReactionType.UP;
                exclusivity = reactionRepository.findByCommentIdAndActorIdAndTypeAndEmojiCode(commentId, actor.actorId(), opposite, null)
                        .flatMap(reactionRepository::delete)
                        .then();
            }

            Mono<CommentReactionEntity> upsertMono = reactionRepository.findByCommentIdAndActorIdAndTypeAndEmojiCode(commentId, actor.actorId(), type, emojiCode)
                    .flatMap(existing -> activate
                            ? Mono.just(existing)
                            : reactionRepository.delete(existing).then(Mono.<CommentReactionEntity>empty()))
                    .switchIfEmpty(Mono.defer(() -> {
                        if (!activate) {
                            return Mono.empty();
                        }
                        var entity = CommentReactionEntity.builder()
                                .commentId(commentId)
                                .actorId(actor.actorId())
                                .actorUrn(actor.actorUrn())
                                .type(type)
                                .emojiCode(emojiCode)
                                .createdAt(OffsetDateTime.now())
                                .build();
                        return reactionRepository.save(entity);
                    }));
            return exclusivity.then(upsertMono).then(getReactions(commentId, actor));
        });
    }

    @Override
    public Mono<ReactionSummaryResponse> getReactions(Long commentId, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        Long actorId = actor != null ? actor.actorId() : null;
        return reactionRepository.findByCommentId(commentId)
                .collectList()
                .map(entities -> toSummaryResponse(entities, actorId));
    }

    private String resolveEmojiCode(ReactionType type, ReactionToggleRequest request) {
        if (type == ReactionType.EMOJI) {
            String code = request.getEmojiCode();
            if (!StringUtils.hasText(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emoji_code required when type is emoji");
            }
            return code.trim();
        }
        if (StringUtils.hasText(request.getEmojiCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emoji_code is only supported for emoji reactions");
        }
        return null;
    }

    private ReactionSummaryResponse toSummaryResponse(List<CommentReactionEntity> entities, Long actorId) {
        int upCount = (int) entities.stream()
                .filter(entity -> entity.getType() == ReactionType.UP)
                .count();
        int downCount = (int) entities.stream()
                .filter(entity -> entity.getType() == ReactionType.DOWN)
                .count();

        Map<String, Long> emojiCounts = entities.stream()
                .filter(entity -> entity.getType() == ReactionType.EMOJI && StringUtils.hasText(entity.getEmojiCode()))
                .collect(Collectors.groupingBy(entity -> entity.getEmojiCode().trim(), Collectors.counting()));

        List<EmojiSummary> emojiSummary = emojiCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> EmojiSummary.builder()
                        .code(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .toList();

        boolean myUp = actorId != null && entities.stream()
                .anyMatch(entity -> entity.getType() == ReactionType.UP && Objects.equals(actorId, entity.getActorId()));
        boolean myDown = actorId != null && entities.stream()
                .anyMatch(entity -> entity.getType() == ReactionType.DOWN && Objects.equals(actorId, entity.getActorId()));
        List<String> myEmoji = actorId != null
                ? entities.stream()
                .filter(entity -> entity.getType() == ReactionType.EMOJI && Objects.equals(actorId, entity.getActorId()))
                .map(entity -> entity.getEmojiCode() != null ? entity.getEmojiCode().trim() : null)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList()
                : List.of();

        var summary = Summary.builder()
                .up(upCount)
                .down(downCount)
                .emoji(emojiSummary)
                .build();

        var myReaction = MyReaction.builder()
                .up(myUp)
                .down(myDown)
                .emoji(myEmoji)
                .build();

        return ReactionSummaryResponse.builder()
                .summary(summary)
                .myReaction(myReaction)
                .build();
    }
}
