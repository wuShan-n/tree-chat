package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.SubjectPatchRequest;
import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.api.dto.SubjectUpsertRequest;
import com.example.commentdemo.comment.domain.entity.CommentSubjectEntity;
import com.example.commentdemo.comment.repository.CommentSubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultSubjectService implements SubjectService {

    private final CommentSubjectRepository repository;
    private final SubjectMapper subjectMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<SubjectResponse> upsert(String subjectKey, SubjectUpsertRequest request) {
        Assert.hasText(subjectKey, "subjectKey must not be blank");

        return repository.findBySubjectKey(subjectKey)
                .flatMap(existing -> applyUpsert(existing, request)
                        .flatMap(repository::save))
                .switchIfEmpty(Mono.defer(() -> createNew(subjectKey, request)
                        .flatMap(repository::save)))
                .map(subjectMapper::toResponse);
    }

    @Override
    public Mono<SubjectResponse> findByKey(String subjectKey) {
        Assert.hasText(subjectKey, "subjectKey must not be blank");

        return repository.findBySubjectKey(subjectKey)
                .map(subjectMapper::toResponse)
                .switchIfEmpty(Mono.error(() -> notFound("Subject not found for key %s".formatted(subjectKey))));
    }

    @Override
    public Mono<SubjectResponse> findById(UUID subjectId) {
        return repository.findById(subjectId)
                .map(subjectMapper::toResponse)
                .switchIfEmpty(Mono.error(() -> notFound("Subject not found for id %s".formatted(subjectId))));
    }

    @Override
    public Mono<SubjectResponse> patch(UUID subjectId, SubjectPatchRequest request) {
        return repository.findById(subjectId)
                .switchIfEmpty(Mono.error(() -> notFound("Subject not found for id %s".formatted(subjectId))))
                .flatMap(existing -> applyPatch(existing, request)
                        .flatMap(repository::save))
                .map(subjectMapper::toResponse);
    }

    private Mono<CommentSubjectEntity> createNew(String subjectKey, SubjectUpsertRequest request) {
        var entity = CommentSubjectEntity.builder()
                .subjectKey(subjectKey)
                .locked(request != null && Boolean.TRUE.equals(request.getLocked()))
                .archived(request != null && Boolean.TRUE.equals(request.getArchived()))
                .policy(request != null ? normalizePolicy(request.getPolicy()) : emptyPolicy())
                .commentCount(0)
                .visibleCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
        return Mono.just(entity);
    }

    private Mono<CommentSubjectEntity> applyUpsert(CommentSubjectEntity entity, SubjectUpsertRequest request) {
        if (request == null) {
            return Mono.just(entity);
        }
        entity.setLocked(request.getLocked() != null ? request.getLocked() : Boolean.FALSE);
        entity.setArchived(request.getArchived() != null ? request.getArchived() : Boolean.FALSE);
        entity.setPolicy(request.getPolicy() != null ? normalizePolicy(request.getPolicy()) : emptyPolicy());
        return Mono.just(entity);
    }

    private Mono<CommentSubjectEntity> applyPatch(CommentSubjectEntity entity, SubjectPatchRequest request) {
        if (request == null) {
            return Mono.just(entity);
        }
        if (request.getLocked() != null) {
            entity.setLocked(request.getLocked());
        }
        if (request.getArchived() != null) {
            entity.setArchived(request.getArchived());
        }
        if (request.getPolicy() != null) {
            entity.setPolicy(normalizePolicy(request.getPolicy()));
        }
        return Mono.just(entity);
    }

    private JsonNode normalizePolicy(JsonNode policy) {
        if (policy == null || policy.isNull()) {
            return emptyPolicy();
        }
        return policy;
    }

    private ObjectNode emptyPolicy() {
        return objectMapper.createObjectNode();
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
