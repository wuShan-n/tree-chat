package com.example.commentdemo.comment.repository;

import com.example.commentdemo.comment.domain.entity.CommentEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface CommentRepository extends ReactiveCrudRepository<CommentEntity, Long> {

    Flux<CommentEntity> findBySubjectId(UUID subjectId);

    @Query("""
            SELECT * FROM comment
            WHERE subject_id = :subjectId AND depth = 0
            ORDER BY created_at DESC
            LIMIT :limit
            """)
    Flux<CommentEntity> findTopLevelComments(@Param("subjectId") UUID subjectId,
                                             @Param("limit") int limit);
}
