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
        return toResponse(entity, true, null, null, null);
    }

    public CommentResponse toResponse(CommentEntity entity,
                                      boolean includeCounts,
                                      CommentResponse.MyReaction myReaction,
                                      CommentResponse.Quality quality,
                                      CommentResponse.Collapse collapse) {
        if (entity == null) {
            return null;
        }

        CommentResponse.Counters counters = includeCounts
                ? CommentResponse.Counters.builder()
                .up(entity.getUpCount() == null ? 0 : entity.getUpCount())
                .down(entity.getDownCount() == null ? 0 : entity.getDownCount())
                .replies(entity.getReplyCount() == null ? 0 : entity.getReplyCount())
                .build()
                : null;

        Long rootId = entity.getRootId() != null ? entity.getRootId() : entity.getId();

        return CommentResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubjectId())
                .rootId(rootId)
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
                .counters(counters)
                .myReaction(myReaction)
                .quality(quality)
                .collapsed(collapse)
                .createdAt(entity.getCreatedAt())
                .editedAt(entity.getEditedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
