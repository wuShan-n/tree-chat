package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.CommentCreateRequest;
import com.example.commentdemo.comment.api.dto.CommentPageResponse;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.api.dto.CommentUpdateRequest;
import com.example.commentdemo.comment.domain.entity.CommentEntity;
import com.example.commentdemo.comment.model.CommentStatus;
import com.example.commentdemo.comment.repository.CommentRepository;
import com.example.commentdemo.comment.repository.CommentSubjectRepository;
import com.example.commentdemo.comment.security.ActorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCommentService implements CommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CommentRepository commentRepository;
    private final CommentSubjectRepository subjectRepository;
    private final CommentMapper commentMapper;

    @Override
    public Mono<CommentPageResponse> listTopLevel(UUID subjectId, String view, int limit, String cursor, ActorContext actor) {
        Assert.notNull(subjectId, "subjectId must not be null");
        int pageSize = normalizeLimit(limit);
        // TODO paging & cursor support
        Flux<CommentEntity> flux = commentRepository.findTopLevelComments(subjectId, pageSize);
        return flux.map(commentMapper::toResponse)
                .collectList()
                .map(items -> CommentPageResponse.builder()
                        .items(items)
                        .nextCursor(null)
                        .build());
    }

    @Override
    public Mono<CommentPageResponse> listReplies(Long commentId, String order, int limit, String cursor, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        // TODO implement structural ordering queries
        return Mono.just(CommentPageResponse.builder()
                .items(List.of())
                .nextCursor(null)
                .build());
    }

    @Override
    public Mono<CommentResponse> getComment(Long commentId, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        return commentRepository.findById(commentId)
                .map(commentMapper::toResponse)
                .switchIfEmpty(Mono.error(() -> notFound("Comment not found for id %s".formatted(commentId))));
    }

    @Override
    public Mono<CommentResponse> create(UUID subjectId, CommentCreateRequest request, ActorContext actor, String idempotencyKey) {
        Assert.notNull(subjectId, "subjectId must not be null");
        Assert.notNull(request, "request must not be null");

        return ensureSubjectExists(subjectId)
                .then(Mono.defer(() -> {
                    if (actor == null || actor.actorId() == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Actor id required for comment creation"));
                    }
                    var entity = CommentEntity.builder()
                            .subjectId(subjectId)
                            .parentId(request.getParentId())
                            .authorId(actor.actorId())
                            .authorUrn(actor.actorUrn())
                            .bodyMd(request.getBodyMd())
                            .bodyHtml(request.getBodyHtml())
                            .status(CommentStatus.PUBLISHED)
                            .upCount(0)
                            .downCount(0)
                            .replyCount(0)
                            .createdAt(OffsetDateTime.now())
                            .build();

                    return commentRepository.save(entity)
                            .map(commentMapper::toResponse);
                }));
    }

    @Override
    public Mono<CommentResponse> update(Long commentId, CommentUpdateRequest request, ActorContext actor, String ifMatch) {
        return Mono.error(new UnsupportedOperationException("Comment update is not implemented yet"));
    }

    private Mono<Void> ensureSubjectExists(UUID subjectId) {
        return subjectRepository.existsById(subjectId)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(() -> notFound("Subject not found for id %s".formatted(subjectId))));
    }

    private int normalizeLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
