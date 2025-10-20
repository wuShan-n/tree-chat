package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.security.ActorContext;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultCommentEventService implements CommentEventService {

    @Override
    public Flux<ServerSentEvent<String>> streamSubjectEvents(UUID subjectId, OffsetDateTime since, Set<String> eventTypes, ActorContext actor) {
        return Flux.empty();
    }
}
