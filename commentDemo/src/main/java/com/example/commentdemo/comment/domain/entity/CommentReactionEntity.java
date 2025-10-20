package com.example.commentdemo.comment.domain.entity;

import com.example.commentdemo.comment.model.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/**
 * Persistence entity mapping {@code comment_reaction}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("comment_reaction")
public class CommentReactionEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("comment_id")
    private Long commentId;

    @Column("actor_id")
    private Long actorId;

    @Column("actor_urn")
    private String actorUrn;

    @Column("type")
    private ReactionType type;

    @Column("emoji_code")
    private String emojiCode;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
