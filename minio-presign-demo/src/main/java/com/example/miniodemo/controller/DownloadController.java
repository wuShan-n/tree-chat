package com.example.miniodemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class DownloadController {
    private final S3Presigner presigner;
    @Value("${app.bucket}") String bucket;

    // 简化演示：根据 id 推导 key；生产建议从 DB 查询具体变体
    @GetMapping("/images/{id}/download")
    public ResponseEntity<Void> download(@PathVariable long id, @RequestParam(defaultValue = "original") String variant) {
        String key = "images/%d/%s".formatted(id, variant.equals("original") ? "original" : "w1024.webp");

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket).key(key)
                .responseContentDisposition("inline")
                .build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(get).build();
        PresignedGetObjectRequest url = presigner.presignGetObject(req);

        return ResponseEntity.status(302).location(URI.create(url.url().toString())).build();
    }
}