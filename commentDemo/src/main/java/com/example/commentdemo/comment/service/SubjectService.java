package com.example.commentdemo.comment.service;

import com.example.commentdemo.comment.api.dto.SubjectMetricsResponse;
import com.example.commentdemo.comment.api.dto.SubjectPatchRequest;
import com.example.commentdemo.comment.api.dto.SubjectResponse;
import com.example.commentdemo.comment.api.dto.SubjectUpsertRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SubjectService {

    Mono<UpsertResult> upsert(String subjectKey, SubjectUpsertRequest request);

    Mono<SubjectResponse> findByKey(String subjectKey);

    Mono<SubjectResponse> findById(UUID subjectId);

    Mono<SubjectResponse> patch(UUID subjectId, SubjectPatchRequest request, String ifMatch);

    Mono<SubjectMetricsResponse> fetchMetrics(UUID subjectId);

    record UpsertResult(SubjectResponse subject, boolean created) {
    }
}
