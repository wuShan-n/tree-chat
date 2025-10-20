package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.security.ActorContextResolver;
import com.example.commentdemo.comment.service.CommentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/comments/v1")
@RequiredArgsConstructor
public class EventStreamController {

    private final CommentEventService eventService;
    private final ActorContextResolver actorContextResolver;

    @GetMapping(value = "/subjects/{subjectId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(@PathVariable("subjectId") UUID subjectId,
                                                      @RequestParam(name = "since", required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
                                                      @RequestParam(name = "types", required = false) String types,
                                                      ServerWebExchange exchange) {
        Set<String> typeFilter = parseTypes(types);
        return actorContextResolver.resolve(exchange)
                .flatMapMany(actor -> eventService.streamSubjectEvents(subjectId, since, typeFilter, actor));
    }

    private Set<String> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
