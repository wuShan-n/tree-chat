package com.example.videodemo.controller.dto;

public record PresignUploadResponse(
        String url,
        String bucket,
        String objectName,
        String method,
        String contentType) {
}
