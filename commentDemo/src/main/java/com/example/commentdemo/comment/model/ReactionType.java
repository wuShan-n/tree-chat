package com.example.commentdemo.comment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Reaction type enumeration aligned with database enum `reaction_type`.
 */
public enum ReactionType {

    UP("up"),
    DOWN("down"),
    EMOJI("emoji");

    private final String value;

    ReactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReactionType fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        var normalized = raw.toLowerCase(Locale.ROOT);
        for (var type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reaction type: " + raw);
    }
}
