package com.example.videodemo.controller.vo;

public record PresignUploadResponse(
        String url,
        String bucket,
        String objectName,
        String method,
        String contentType) {
}
