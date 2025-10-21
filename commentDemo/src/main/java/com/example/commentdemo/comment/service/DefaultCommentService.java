package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.CommentCreateRequest;
import com.example.commentdemo.comment.api.dto.CommentPageResponse;
import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.api.dto.CommentUpdateRequest;
import com.example.commentdemo.comment.domain.entity.CommentEntity;
import com.example.commentdemo.comment.domain.entity.CommentReactionEntity;
import com.example.commentdemo.comment.model.CommentStatus;
import com.example.commentdemo.comment.model.ReactionType;
import com.example.commentdemo.comment.repository.CommentReactionRepository;
import com.example.commentdemo.comment.repository.CommentRepository;
import com.example.commentdemo.comment.repository.CommentSubjectRepository;
import com.example.commentdemo.comment.security.ActorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCommentService implements CommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_TOP_LEVEL_PAGE_SIZE = 100;
    private static final int MAX_REPLY_PAGE_SIZE = 200;
    private static final String ROLE_MODERATOR = "comment:moderator";
    private static final String ROLE_ADMIN = "comment:admin";

    private final CommentRepository commentRepository;
    private final CommentSubjectRepository subjectRepository;
    private final CommentReactionRepository reactionRepository;
    private final CommentMapper commentMapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<CommentPageResponse> listTopLevel(UUID subjectId,
                                                  String view,
                                                  int limit,
                                                  String cursor,
                                                  String status,
                                                  boolean withCounts,
                                                  boolean withMyReaction,
                                                  ActorContext actor) {
        Assert.notNull(subjectId, "subjectId must not be null");
        ViewMode viewMode = ViewMode.from(view);
        StatusFilter statusFilter = StatusFilter.from(status);
        PageCursor pageCursor = decodeCursor(cursor);
        int pageSize = normalizeLimit(limit, MAX_TOP_LEVEL_PAGE_SIZE);

        return ensureSubjectExists(subjectId)
                .thenMany(fetchTopLevelIds(subjectId, viewMode, statusFilter, pageCursor, pageSize + 1))
                .concatMap(commentRepository::findById)
                .collectList()
                .flatMap(entities -> buildPageResponse(entities, pageSize, withCounts, withMyReaction, 0, actor));
    }

    @Override
    public Mono<CommentPageResponse> listReplies(Long commentId,
                                                 String order,
                                                 int limit,
                                                 String cursor,
                                                 double collapseBelow,
                                                 boolean withCounts,
                                                 boolean withMyReaction,
                                                 ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        ReplyOrder replyOrder = ReplyOrder.from(order);
        PageCursor pageCursor = decodeCursor(cursor);
        int pageSize = normalizeLimit(limit, MAX_REPLY_PAGE_SIZE);

        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(() -> notFound("Comment not found for id %s".formatted(commentId))))
                .flatMap(parent -> fetchReplyIds(commentId, replyOrder, pageCursor, pageSize + 1)
                        .concatMap(commentRepository::findById)
                        .collectList()
                        .flatMap(entities -> buildPageResponse(entities, pageSize, withCounts, withMyReaction, collapseBelow, actor)));
    }

    @Override
    public Mono<CommentResponse> getComment(Long commentId, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");
        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(() -> notFound("Comment not found for id %s".formatted(commentId))))
                .flatMap(entity -> enrichSingle(entity, true, true, 0, actor));
    }

    @Override
    public Mono<CommentResponse> create(UUID subjectId, CommentCreateRequest request, ActorContext actor, String idempotencyKey) {
        Assert.notNull(subjectId, "subjectId must not be null");
        Assert.notNull(request, "request must not be null");

        return ensureSubjectExists(subjectId)
                .then(requireParent(subjectId, request.getParentId()))
                .flatMap(parent -> {
                    requireActor(actor, "Actor id required for comment creation");
                    CommentStatus status = resolveCreateStatus(request.getStatus());
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                    CommentEntity entity = CommentEntity.builder()
                            .subjectId(subjectId)
                            .parentId(parent.parentId())
                            .authorId(actor.actorId())
                            .authorUrn(actor.actorUrn())
                            .bodyMd(request.getBodyMd())
                            .bodyHtml(request.getBodyHtml())
                            .status(status)
                            .upCount(0)
                            .downCount(0)
                            .replyCount(0)
                            .createdAt(now)
                            .build();

                    return commentRepository.save(entity)
                            .flatMap(saved -> getComment(saved.getId(), actor));
                });
    }

    @Override
    public Mono<CommentResponse> update(Long commentId, CommentUpdateRequest request, ActorContext actor, String ifMatch) {
        Assert.notNull(commentId, "commentId must not be null");
        Assert.notNull(request, "request must not be null");

        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(() -> notFound("Comment not found for id %s".formatted(commentId))))
                .flatMap(existing -> {
                    requireActor(actor, "Actor id required for comment updates");
                    enforceOwnership(actor, existing);
                    validateIfMatch(ifMatch, existing);

                    boolean mutated = false;
                    if (request.getBodyMd() != null && !Objects.equals(request.getBodyMd(), existing.getBodyMd())) {
                        existing.setBodyMd(request.getBodyMd());
                        mutated = true;
                    }
                    if (request.getBodyHtml() != null && !Objects.equals(request.getBodyHtml(), existing.getBodyHtml())) {
                        existing.setBodyHtml(request.getBodyHtml());
                        mutated = true;
                    }
                    if (request.getStatus() != null && request.getStatus() != existing.getStatus()) {
                        CommentStatus newStatus = request.getStatus();
                        validateUpdateStatus(newStatus);
                        existing.setStatus(newStatus);
                        existing.setDeletedAt(newStatus == CommentStatus.DELETED ? OffsetDateTime.now(ZoneOffset.UTC) : null);
                        mutated = true;
                    }
                    if (Boolean.TRUE.equals(request.getHardDelete())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hard_delete is not supported via PATCH; use DELETE instead");
                    }
                    if (!mutated) {
                        return enrichSingle(existing, true, true, 0, actor);
                    }

                    existing.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return commentRepository.save(existing)
                            .flatMap(saved -> getComment(saved.getId(), actor));
                });
    }

    @Override
    public Mono<Void> delete(Long commentId, boolean soft, ActorContext actor) {
        Assert.notNull(commentId, "commentId must not be null");

        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(() -> notFound("Comment not found for id %s".formatted(commentId))))
                .flatMap(existing -> {
                    requireActor(actor, "Actor id required for comment deletion");
                    enforceOwnership(actor, existing);

                    if (soft) {
                        existing.setStatus(CommentStatus.DELETED);
                        existing.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        existing.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        return commentRepository.save(existing).then();
                    }
                    return commentRepository.delete(existing);
                });
    }

    private Mono<Void> ensureSubjectExists(UUID subjectId) {
        return subjectRepository.existsById(subjectId)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(() -> notFound("Subject not found for id %s".formatted(subjectId))));
    }

    private Mono<ParentContext> requireParent(UUID subjectId, Long parentId) {
        if (parentId == null) {
            return Mono.just(ParentContext.root());
        }
        return commentRepository.findById(parentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment not found")))
                .flatMap(parent -> {
                    if (!subjectId.equals(parent.getSubjectId())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment belongs to a different subject"));
                    }
                    if (parent.getStatus() == CommentStatus.DELETED) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment is deleted"));
                    }
                    return Mono.just(new ParentContext(parent.getId()));
                });
    }

    private Flux<Long> fetchTopLevelIds(UUID subjectId,
                                        ViewMode viewMode,
                                        StatusFilter statusFilter,
                                        PageCursor cursor,
                                        int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id FROM comment c ");
        if (viewMode.requiresRank()) {
            sql.append("JOIN comment_rank_mv r ON r.id = c.id ");
        }
        sql.append("WHERE c.subject_id = :subjectId ");
        sql.append("AND (c.parent_id IS NULL OR c.depth = 0)");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subjectId", subjectId);
        applyStatusFilter(sql, params, statusFilter, "c");
        appendCursorClause(sql, params, cursor, viewMode.isAscending());
        sql.append(" ORDER BY ").append(viewMode.orderClause());
        sql.append(" LIMIT :limit");
        params.put("limit", limit);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }
        return spec.map((row, metadata) -> row.get("id", Long.class))
                .all();
    }

    private Flux<Long> fetchReplyIds(Long commentId,
                                     ReplyOrder order,
                                     PageCursor cursor,
                                     int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id FROM comment c WHERE c.parent_id = :parentId");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("parentId", commentId);
        sql.append(" AND c.status <> 'deleted'");
        appendCursorClause(sql, params, cursor, true);
        sql.append(" ORDER BY ").append(order.orderClause());
        sql.append(" LIMIT :limit");
        params.put("limit", limit);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }
        return spec.map((row, metadata) -> row.get("id", Long.class))
                .all();
    }

    private Mono<CommentPageResponse> buildPageResponse(List<CommentEntity> entities,
                                                        int pageSize,
                                                        boolean includeCounts,
                                                        boolean includeMyReaction,
                                                        double collapseBelow,
                                                        ActorContext actor) {
        if (entities.isEmpty()) {
            return Mono.just(CommentPageResponse.builder()
                    .items(List.of())
                    .nextCursor(null)
                    .build());
        }

        boolean hasNext = entities.size() > pageSize;
        List<CommentEntity> pageItems = hasNext ? entities.subList(0, pageSize) : entities;
        List<Long> commentIds = pageItems.stream()
                .map(CommentEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        Mono<Map<Long, CommentResponse.MyReaction>> reactionsMono = fetchMyReactions(commentIds, actor, includeMyReaction);
        Mono<Map<Long, CommentResponse.Quality>> qualityMono = fetchQuality(commentIds);

        return Mono.zip(reactionsMono.defaultIfEmpty(Map.of()), qualityMono.defaultIfEmpty(Map.of()))
                .map(tuple -> toPageResponse(tuple, pageItems, includeCounts, collapseBelow, hasNext));
    }

    private CommentPageResponse toPageResponse(Tuple2<Map<Long, CommentResponse.MyReaction>, Map<Long, CommentResponse.Quality>> tuple,
                                               List<CommentEntity> pageItems,
                                               boolean includeCounts,
                                               double collapseBelow,
                                               boolean hasNext) {
        Map<Long, CommentResponse.MyReaction> reactions = tuple.getT1();
        Map<Long, CommentResponse.Quality> quality = tuple.getT2();

        List<CommentResponse> responses = new ArrayList<>(pageItems.size());
        for (CommentEntity entity : pageItems) {
            Long id = entity.getId();
            CommentResponse.Quality q = quality.get(id);
            CommentResponse.Collapse collapse = computeCollapse(q, collapseBelow);
            responses.add(commentMapper.toResponse(
                    entity,
                    includeCounts,
                    reactions.get(id),
                    q,
                    collapse
            ));
        }

        String nextCursor = hasNext && !pageItems.isEmpty()
                ? encodeCursor(pageItems.get(pageItems.size() - 1))
                : null;

        return CommentPageResponse.builder()
                .items(responses)
                .nextCursor(nextCursor)
                .build();
    }

    private Mono<CommentResponse> enrichSingle(CommentEntity entity,
                                               boolean includeCounts,
                                               boolean includeMyReaction,
                                               double collapseBelow,
                                               ActorContext actor) {
        if (entity.getId() == null) {
            return Mono.just(commentMapper.toResponse(entity, includeCounts, null, null, null));
        }
        List<Long> ids = List.of(entity.getId());
        Mono<Map<Long, CommentResponse.MyReaction>> reactions = fetchMyReactions(ids, actor, includeMyReaction);
        Mono<Map<Long, CommentResponse.Quality>> quality = fetchQuality(ids);

        return Mono.zip(reactions.defaultIfEmpty(Map.of()), quality.defaultIfEmpty(Map.of()))
                .map(tuple -> {
                    CommentResponse.MyReaction myReaction = tuple.getT1().get(entity.getId());
                    CommentResponse.Quality qualityValue = tuple.getT2().get(entity.getId());
                    CommentResponse.Collapse collapse = computeCollapse(qualityValue, collapseBelow);
                    return commentMapper.toResponse(entity, includeCounts, myReaction, qualityValue, collapse);
                });
    }

    private Mono<Map<Long, CommentResponse.MyReaction>> fetchMyReactions(List<Long> commentIds,
                                                                         ActorContext actor,
                                                                         boolean include) {
        if (!include || actor == null || actor.actorId() == null || commentIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        return reactionRepository.findByCommentIdInAndActorId(commentIds, actor.actorId())
                .collectList()
                .map(entities -> {
                    Map<Long, ReactionAccumulator> accumulators = new LinkedHashMap<>();
                    for (CommentReactionEntity reaction : entities) {
                        ReactionAccumulator accumulator = accumulators.computeIfAbsent(
                                reaction.getCommentId(),
                                key -> new ReactionAccumulator()
                        );
                        if (reaction.getType() == ReactionType.UP) {
                            accumulator.up = true;
                        } else if (reaction.getType() == ReactionType.DOWN) {
                            accumulator.down = true;
                        } else if (reaction.getType() == ReactionType.EMOJI && StringUtils.hasText(reaction.getEmojiCode())) {
                            accumulator.emoji.add(reaction.getEmojiCode().trim());
                        }
                    }
                    Map<Long, CommentResponse.MyReaction> result = new LinkedHashMap<>();
                    accumulators.forEach((commentId, accumulator) -> result.put(commentId, accumulator.toResponse()));
                    return result;
                });
    }

    private Mono<Map<Long, CommentResponse.Quality>> fetchQuality(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        StringBuilder sql = new StringBuilder("SELECT id, best_score, hot_score FROM comment_rank_mv WHERE id IN (");
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < commentIds.size(); i++) {
            joiner.add(":q" + i);
        }
        sql.append(joiner).append(")");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (int i = 0; i < commentIds.size(); i++) {
            spec = spec.bind("q" + i, commentIds.get(i));
        }

        return spec.map((row, metadata) -> {
                    Long id = row.get("id", Long.class);
                    Number best = row.get("best_score", Number.class);
                    Number hot = row.get("hot_score", Number.class);
                    CommentResponse.Quality quality = CommentResponse.Quality.builder()
                            .bestScore(best != null ? BigDecimal.valueOf(best.doubleValue()) : null)
                            .hotScore(hot != null ? BigDecimal.valueOf(hot.doubleValue()) : null)
                            .build();
                    return Map.entry(id, quality);
                })
                .all()
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private CommentResponse.Collapse computeCollapse(CommentResponse.Quality quality, double threshold) {
        if (threshold <= 0 || quality == null || quality.bestScore() == null) {
            return null;
        }
        if (quality.bestScore().doubleValue() >= threshold) {
            return null;
        }
        return CommentResponse.Collapse.builder()
                .value(true)
                .reason("score_below_threshold")
                .build();
    }

    private void requireActor(ActorContext actor, String message) {
        if (actor == null || actor.actorId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
    }

    private void enforceOwnership(ActorContext actor, CommentEntity entity) {
        if (actor == null || entity == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor is not allowed to mutate this comment");
        }
        if (actor.hasRole(ROLE_ADMIN) || actor.hasRole(ROLE_MODERATOR)) {
            return;
        }
        if (entity.getAuthorId() == null || !entity.getAuthorId().equals(actor.actorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor is not allowed to mutate this comment");
        }
    }

    private void validateIfMatch(String ifMatch, CommentEntity entity) {
        if (!StringUtils.hasText(ifMatch)) {
            return;
        }
        String expected = computeEtag(entity);
        if (!expected.equals(ifMatch.trim())) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Stale comment representation");
        }
    }

    private CommentStatus resolveCreateStatus(CommentStatus requested) {
        if (requested == null || requested == CommentStatus.PUBLISHED) {
            return CommentStatus.PUBLISHED;
        }
        if (requested == CommentStatus.PENDING) {
            return CommentStatus.PENDING;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New comments may only be published or pending");
    }

    private void validateUpdateStatus(CommentStatus status) {
        if (status == CommentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transition comment back to pending");
        }
    }

    private int normalizeLimit(int requested, int maximum) {
        int resolved = requested <= 0 ? DEFAULT_PAGE_SIZE : requested;
        return Math.min(resolved, maximum);
    }

    private void applyStatusFilter(StringBuilder sql, Map<String, Object> params, StatusFilter filter, String alias) {
        if (filter == StatusFilter.ALL) {
            return;
        }
        if (filter == StatusFilter.PUBLISHED) {
            sql.append(" AND ").append(alias).append(".status = 'published' AND ").append(alias).append(".deleted_at IS NULL");
            return;
        }
        sql.append(" AND ").append(alias).append(".status = :statusFilter");
        params.put("statusFilter", filter.value());
    }

    private void appendCursorClause(StringBuilder sql, Map<String, Object> params, PageCursor cursor, boolean ascending) {
        if (cursor == null) {
            return;
        }
        sql.append(" AND (c.created_at, c.id) ");
        sql.append(ascending ? ">" : "<");
        sql.append(" (:cursorCreatedAt, :cursorId)");
        params.put("cursorCreatedAt", cursor.createdAt());
        params.put("cursorId", cursor.id());
    }

    private PageCursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor format invalid");
            }
            long epochMillis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            OffsetDateTime createdAt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
            return new PageCursor(createdAt, id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor", ex);
        }
    }

    private String encodeCursor(CommentEntity entity) {
        OffsetDateTime createdAt = entity.getCreatedAt() != null
                ? entity.getCreatedAt()
                : OffsetDateTime.now(ZoneOffset.UTC);
        long epochMillis = createdAt.toInstant().toEpochMilli();
        String payload = epochMillis + ":" + entity.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String computeEtag(CommentEntity entity) {
        OffsetDateTime baseline = entity.getEditedAt();
        if (baseline == null) {
            baseline = entity.getDeletedAt();
        }
        if (baseline == null) {
            baseline = entity.getCreatedAt();
        }
        long version = baseline != null ? baseline.toInstant().toEpochMilli() : 0L;
        String token = entity.getId() + ":" + version;
        return "W/\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8)) + "\"";
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private enum ViewMode {
        BEST("best", false, true, "r.best_score DESC NULLS LAST, c.created_at DESC, c.id DESC"),
        HOT("hot", false, true, "r.hot_score DESC NULLS LAST, c.created_at DESC, c.id DESC"),
        NEW("new", false, false, "c.created_at DESC, c.id DESC"),
        OLD("old", true, false, "c.created_at ASC, c.id ASC"),
        CONTROVERSIAL("controversial", false, false, "LEAST(c.up_count, c.down_count) DESC, c.created_at DESC, c.id DESC");

        private final String value;
        private final boolean ascending;
        private final boolean requiresRank;
        private final String orderClause;

        ViewMode(String value, boolean ascending, boolean requiresRank, String orderClause) {
            this.value = value;
            this.ascending = ascending;
            this.requiresRank = requiresRank;
            this.orderClause = orderClause;
        }

        static ViewMode from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return BEST;
            }
            String candidate = raw.toLowerCase(Locale.ROOT);
            for (ViewMode mode : values()) {
                if (mode.value.equals(candidate)) {
                    return mode;
                }
            }
            return BEST;
        }

        boolean isAscending() {
            return ascending;
        }

        boolean requiresRank() {
            return requiresRank;
        }

        String orderClause() {
            return orderClause;
        }
    }

    private enum StatusFilter {
        PUBLISHED("published"),
        ALL("all"),
        PENDING("pending"),
        HIDDEN("hidden"),
        DELETED("deleted"),
        SPAM("spam");

        private final String value;

        StatusFilter(String value) {
            this.value = value;
        }

        static StatusFilter from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return PUBLISHED;
            }
            String candidate = raw.toLowerCase(Locale.ROOT);
            for (StatusFilter filter : values()) {
                if (filter.value.equals(candidate)) {
                    return filter;
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter: " + raw);
        }

        String value() {
            return value;
        }
    }

    private enum ReplyOrder {
        STRUCTURE("structure", "c.path ASC, c.id ASC"),
        CHRONOLOGICAL("chronological", "c.created_at ASC, c.id ASC");

        private final String value;
        private final String orderClause;

        ReplyOrder(String value, String orderClause) {
            this.value = value;
            this.orderClause = orderClause;
        }

        static ReplyOrder from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return STRUCTURE;
            }
            String candidate = raw.toLowerCase(Locale.ROOT);
            for (ReplyOrder order : values()) {
                if (order.value.equals(candidate)) {
                    return order;
                }
            }
            return STRUCTURE;
        }

        String orderClause() {
            return orderClause;
        }
    }

    private record PageCursor(OffsetDateTime createdAt, long id) {
    }

    private record ParentContext(Long parentId) {

        static ParentContext root() {
            return new ParentContext(null);
        }
    }

    private static final class ReactionAccumulator {

        private boolean up;
        private boolean down;
        private final Set<String> emoji = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        CommentResponse.MyReaction toResponse() {
            Set<String> codes = emoji.isEmpty() ? Set.of() : new LinkedHashSet<>(emoji);
            return CommentResponse.MyReaction.builder()
                    .up(up)
                    .down(down)
                    .emoji(codes)
                    .build();
        }
    }
}
