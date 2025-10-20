package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.CommentResponse;
import com.example.commentdemo.comment.domain.entity.CommentEntity;
import org.springframework.stereotype.Component;

/**
 * Maps comment entities to API responses.
 */
@Component
public class CommentMapper {

    public CommentResponse toResponse(CommentEntity entity) {
        if (entity == null) {
            return null;
        }

        return CommentResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubjectId())
                .rootId(entity.getRootId())
                .parentId(entity.getParentId())
                .depth(entity.getDepth() == null ? 0 : entity.getDepth())
                .author(CommentResponse.Author.builder()
                        .id(entity.getAuthorId())
                        .urn(entity.getAuthorUrn())
                        .build())
                .bodyMd(entity.getBodyMd())
                .bodyHtml(entity.getBodyHtml())
                .status(entity.getStatus())
                .toxicityScore(entity.getToxicityScore())
                .counters(CommentResponse.Counters.builder()
                        .up(entity.getUpCount() == null ? 0 : entity.getUpCount())
                        .down(entity.getDownCount() == null ? 0 : entity.getDownCount())
                        .replies(entity.getReplyCount() == null ? 0 : entity.getReplyCount())
                        .build())
                .createdAt(entity.getCreatedAt())
                .editedAt(entity.getEditedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
