package com.example.videodemo.service;

import com.example.videodemo.controller.dto.PresignGetResponse;
import com.example.videodemo.controller.dto.PresignUploadRequest;
import com.example.videodemo.controller.dto.PresignUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

@Service
public class UploadPresignService {

    private static final Duration UPLOAD_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);
    private static final String DEFAULT_CONTENT_TYPE = "video/mp4";

    private final S3Presigner presigner;
    private final String bucket;

    public UploadPresignService(S3Presigner presigner, @Value("${app.s3.bucket}") String bucket) {
        this.presigner = presigner;
        this.bucket = bucket;
    }

    public PresignUploadResponse createUploadUrl(PresignUploadRequest request) {
        String objectName = resolveObjectName(request.objectName());
        String contentType = resolveContentType(request.contentType());

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_TTL)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return new PresignUploadResponse(
                presigned.url().toString(),
                bucket,
                objectName,
                presigned.httpRequest().method().name(),
                contentType);
    }

    public PresignGetResponse createDownloadUrl(String objectName) {
        String key = resolveObjectName(objectName);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_TTL)
                .getObjectRequest(request)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return new PresignGetResponse(
                presigned.url().toString(),
                presigned.httpRequest().method().name());
    }

    private String resolveObjectName(String objectName) {
        if (StringUtils.hasText(objectName)) {
            return objectName;
        }
        return "video-" + System.currentTimeMillis() + ".mp4";
    }

    private String resolveContentType(String contentType) {
        return Optional.ofNullable(contentType)
                .filter(StringUtils::hasText)
                .orElse(DEFAULT_CONTENT_TYPE);
    }
}
