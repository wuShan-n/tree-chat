package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.domain.entity.CommentSubjectEntity;
import org.springframework.stereotype.Component;

/**
 * Maps persistence entities to API responses.
 */
@Component
public class SubjectMapper {

    public SubjectResponse toResponse(CommentSubjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return SubjectResponse.builder()
                .subjectId(entity.getSubjectId())
                .subjectKey(entity.getSubjectKey())
                .locked(Boolean.TRUE.equals(entity.getLocked()))
                .archived(Boolean.TRUE.equals(entity.getArchived()))
                .policy(entity.getPolicy())
                .commentCount(entity.getCommentCount() == null ? 0 : entity.getCommentCount())
                .visibleCount(entity.getVisibleCount() == null ? 0 : entity.getVisibleCount())
                .lastCommentedAt(entity.getLastCommentedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
