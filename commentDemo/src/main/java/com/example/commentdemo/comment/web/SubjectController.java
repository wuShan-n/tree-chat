package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.SubjectPatchRequest;
import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.api.dto.SubjectUpsertRequest;
import com.example.commentdemo.comment.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/comments/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PutMapping("/subjects/{subjectKey}")
    public Mono<SubjectResponse> upsertSubject(@PathVariable("subjectKey") String subjectKey,
                                               @Valid @RequestBody(required = false) SubjectUpsertRequest request) {
        return subjectService.upsert(subjectKey, request);
    }

    @GetMapping("/subjects/{identifier}")
    public Mono<SubjectResponse> getSubject(@PathVariable("identifier") String identifier) {
        return parseUuid(identifier)
                .map(subjectService::findById)
                .orElseGet(() -> subjectService.findByKey(identifier));
    }

    @PatchMapping("/subjects/{subjectId}")
    public Mono<SubjectResponse> patchSubject(@PathVariable("subjectId") UUID subjectId,
                                              @Valid @RequestBody(required = false) SubjectPatchRequest request) {
        return subjectService.patch(subjectId, request);
    }

    private java.util.Optional<UUID> parseUuid(String raw) {
        try {
            return java.util.Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}
