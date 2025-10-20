package com.example.commentdemo.comment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Comment life-cycle state aligned with the database enum `comment_status`.
 */
public enum CommentStatus {

    PUBLISHED("published"),
    PENDING("pending"),
    HIDDEN("hidden"),
    DELETED("deleted"),
    SPAM("spam");

    private final String value;

    CommentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CommentStatus fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        var normalized = raw.toLowerCase(Locale.ROOT);
        for (var status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown comment status: " + raw);
    }
}
