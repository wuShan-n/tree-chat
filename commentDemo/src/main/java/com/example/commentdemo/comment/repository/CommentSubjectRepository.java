package com.example.commentdemo.comment.repository;

import com.example.commentdemo.comment.domain.entity.CommentSubjectEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CommentSubjectRepository extends ReactiveCrudRepository<CommentSubjectEntity, UUID> {

    Mono<CommentSubjectEntity> findBySubjectKey(String subjectKey);
}
