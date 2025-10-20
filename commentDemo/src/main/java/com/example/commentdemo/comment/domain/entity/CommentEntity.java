package com.example.commentdemo.comment.domain.entity;

import com.example.commentdemo.comment.model.CommentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persistence entity mapping {@code comment}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("comment")
public class CommentEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("subject_id")
    private UUID subjectId;

    @Column("root_id")
    private Long rootId;

    @Column("parent_id")
    private Long parentId;

    @Column("path")
    private String path;

    @Column("author_id")
    private Long authorId;

    @Column("author_urn")
    private String authorUrn;

    @Column("body_md")
    private String bodyMd;

    @Column("body_html")
    private String bodyHtml;

    @Column("status")
    private CommentStatus status;

    @Column("toxicity_score")
    private BigDecimal toxicityScore;

    @Column("up_count")
    private Integer upCount;

    @Column("down_count")
    private Integer downCount;

    @Column("reply_count")
    private Integer replyCount;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("edited_at")
    private OffsetDateTime editedAt;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

    @Column("depth")
    private Integer depth;
}
