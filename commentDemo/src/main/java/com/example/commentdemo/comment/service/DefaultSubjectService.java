package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.SubjectMetricsResponse;
import com.example.commentdemo.comment.api.dto.SubjectPatchRequest;
import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.api.dto.SubjectUpsertRequest;
import com.example.commentdemo.comment.domain.entity.CommentSubjectEntity;
import com.example.commentdemo.comment.repository.CommentSubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultSubjectService implements SubjectService {

    private static final int TOP_CONTRIBUTOR_LIMIT = 5;

    private final CommentSubjectRepository repository;
    private final SubjectMapper subjectMapper;
    private final ObjectMapper objectMapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<UpsertResult> upsert(String subjectKey, SubjectUpsertRequest request) {
        Assert.hasText(subjectKey, "subjectKey must not be blank");

        return repository.findBySubjectKey(subjectKey)
                .flatMap(existing -> applyMutation(existing, toMutation(request))
                        .flatMap(repository::save)
                        .map(saved -> new UpsertResult(subjectMapper.toResponse(saved), false)))
                .switchIfEmpty(Mono.defer(() -> createNew(subjectKey, request)
                        .flatMap(repository::save)
                        .map(saved -> new UpsertResult(subjectMapper.toResponse(saved), true))));
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
        return requireSubject(subjectId)
                .map(subjectMapper::toResponse);
    }

    @Override
    public Mono<SubjectResponse> patch(UUID subjectId, SubjectPatchRequest request, String ifMatch) {
        Assert.notNull(subjectId, "subjectId must not be null");

        return requireSubject(subjectId)
                .flatMap(existing -> applyMutation(existing, toMutation(request))
                        .flatMap(repository::save))
                .map(subjectMapper::toResponse);
    }

    @Override
    public Mono<SubjectMetricsResponse> fetchMetrics(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not be null");

        return requireSubject(subjectId)
                .flatMap(entity -> Mono.zip(
                        computeTopContributors(subjectId).collectList(),
                        computeP95Latency(subjectId))
                        .map(tuple -> new SubjectMetricsResponse(
                                safeCount(entity.getCommentCount()),
                                safeCount(entity.getVisibleCount()),
                                tuple.getT1(),
                                tuple.getT2()
                        )));
    }

    private Mono<CommentSubjectEntity> createNew(String subjectKey, SubjectUpsertRequest request) {
        var mutation = toMutation(request);
        var policy = mutation.policy() != null ? mutation.policy().deepCopy() : emptyPolicy();

        var entity = CommentSubjectEntity.builder()
                .subjectKey(subjectKey)
                .locked(Boolean.TRUE.equals(mutation.locked()))
                .archived(Boolean.TRUE.equals(mutation.archived()))
                .policy(policy)
                .commentCount(0)
                .visibleCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
        return Mono.just(entity);
    }

    private Mono<CommentSubjectEntity> applyMutation(CommentSubjectEntity entity, SubjectMutation mutation) {
        if (mutation.isNoOp()) {
            return Mono.just(entity);
        }

        return Mono.fromSupplier(() -> {
            if (mutation.locked() != null) {
                entity.setLocked(mutation.locked());
            }
            if (mutation.archived() != null) {
                entity.setArchived(mutation.archived());
            }
            if (mutation.policy() != null) {
                entity.setPolicy(mutation.policy().deepCopy());
            }
            return entity;
        });
    }

    private SubjectMutation toMutation(SubjectUpsertRequest request) {
        if (request == null) {
            return SubjectMutation.empty();
        }
        return new SubjectMutation(
                request.getLocked(),
                request.getArchived(),
                request.getPolicy() != null ? normalizePolicy(request.getPolicy()) : null
        );
    }

    private SubjectMutation toMutation(SubjectPatchRequest request) {
        if (request == null) {
            return SubjectMutation.empty();
        }
        return new SubjectMutation(
                request.getLocked(),
                request.getArchived(),
                request.getPolicy() != null ? normalizePolicy(request.getPolicy()) : null
        );
    }

    private ObjectNode normalizePolicy(JsonNode policy) {
        if (policy == null || policy.isNull() || policy.isMissingNode()) {
            return emptyPolicy();
        }
        if (policy instanceof ObjectNode objectNode) {
            return objectNode.deepCopy();
        }
        try {
            return objectMapper.convertValue(policy, ObjectNode.class);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy must be a JSON object", ex);
        }
    }

    private ObjectNode emptyPolicy() {
        return objectMapper.createObjectNode();
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private Flux<SubjectMetricsResponse.TopContributor> computeTopContributors(UUID subjectId) {
        var sql = """
                SELECT author_id AS actor_id, COUNT(*) AS contribution_count
                FROM comment
                WHERE subject_id = :subjectId
                  AND status = 'published'
                  AND deleted_at IS NULL
                GROUP BY author_id
                ORDER BY contribution_count DESC, actor_id ASC
                LIMIT %d
                """.formatted(TOP_CONTRIBUTOR_LIMIT);
        return databaseClient.sql(sql)
                .bind("subjectId", subjectId)
                .map((row, metadata) -> {
                    var actorId = row.get("actor_id", Long.class);
                    var count = row.get("contribution_count", Number.class);
                    return new SubjectMetricsResponse.TopContributor(
                            actorId != null ? actorId : 0L,
                            count != null ? count.intValue() : 0
                    );
                })
                .all();
    }

    private Mono<Integer> computeP95Latency(UUID subjectId) {
        var sql = """
                SELECT COALESCE(
                    PERCENTILE_DISC(0.95) WITHIN GROUP (ORDER BY latency_ms),
                    0
                ) AS latency_ms
                FROM (
                    SELECT EXTRACT(EPOCH FROM (c.created_at - cs.created_at)) * 1000 AS latency_ms
                    FROM comment c
                    JOIN comment_subject cs ON cs.subject_id = c.subject_id
                    WHERE c.subject_id = :subjectId
                      AND c.status = 'published'
                      AND c.deleted_at IS NULL
                ) metrics
                """;
        return databaseClient.sql(sql)
                .bind("subjectId", subjectId)
                .map((row, metadata) -> {
                    var number = row.get("latency_ms", Number.class);
                    return number == null ? 0 : number.intValue();
                })
                .one()
                .defaultIfEmpty(0);
    }

    private Mono<CommentSubjectEntity> requireSubject(UUID subjectId) {
        return repository.findById(subjectId)
                .switchIfEmpty(Mono.error(() -> notFound("Subject not found for id %s".formatted(subjectId))));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record SubjectMutation(Boolean locked, Boolean archived, ObjectNode policy) {

        private static SubjectMutation empty() {
            return new SubjectMutation(null, null, null);
        }

        private boolean isNoOp() {
            return locked == null && archived == null && policy == null;
        }
    }
}
