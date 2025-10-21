package com.example.commentdemo.comment.web;

import com.example.commentdemo.comment.api.dto.SubjectMetricsResponse;
import com.example.commentdemo.comment.api.dto.SubjectPatchRequest;
import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.api.dto.SubjectUpsertRequest;
import com.example.commentdemo.comment.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public Mono<ResponseEntity<SubjectResponse>> upsertSubject(@PathVariable("subjectKey") String subjectKey,
                                                               @Valid @RequestBody(required = false) SubjectUpsertRequest request,
                                                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return subjectService.upsert(subjectKey, request)
                .map(result -> result.created()
                        ? ResponseEntity.status(HttpStatus.CREATED).body(result.subject())
                        : ResponseEntity.ok(result.subject()));
    }

    @GetMapping("/subjects/by-key/{subjectKey}")
    public Mono<SubjectResponse> getSubjectByKey(@PathVariable("subjectKey") String subjectKey) {
        return subjectService.findByKey(subjectKey);
    }

    @GetMapping("/subjects/id/{subjectId}")
    public Mono<SubjectResponse> getSubjectById(@PathVariable("subjectId") UUID subjectId) {
        return subjectService.findById(subjectId);
    }

    @PatchMapping("/subjects/id/{subjectId}")
    public Mono<SubjectResponse> patchSubject(@PathVariable("subjectId") UUID subjectId,
                                              @Valid @RequestBody SubjectPatchRequest request,
                                              @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        return subjectService.patch(subjectId, request, ifMatch);
    }

    @GetMapping("/subjects/id/{subjectId}/metrics")
    public Mono<SubjectMetricsResponse> getSubjectMetrics(@PathVariable("subjectId") UUID subjectId) {
        return subjectService.fetchMetrics(subjectId);
    }
}
