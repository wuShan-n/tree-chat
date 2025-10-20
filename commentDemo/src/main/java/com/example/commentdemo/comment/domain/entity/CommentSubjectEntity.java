package com.example.commentdemo.comment.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persistence entity mapping {@code comment_subject}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("comment_subject")
public class CommentSubjectEntity {

    @Id
    @Column("subject_id")
    private UUID subjectId;

    @Column("subject_key")
    private String subjectKey;

    @Column("is_locked")
    private Boolean locked;

    @Column("is_archived")
    private Boolean archived;

    @Column("policy")
    private JsonNode policy;

    @Column("comment_count")
    private Integer commentCount;

    @Column("visible_count")
    private Integer visibleCount;

    @Column("last_commented_at")
    private OffsetDateTime lastCommentedAt;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
