package com.example.videodemo.service.dto;

public record CreateUploadCommand(
        String title,
        String description,
        String fileName,
        long fileSize,
        String uploadChannel,
        String uploaderId,
        String sourceBucket,
        String sourceObject,
        String ingestStrategy,
        String checksum) {
}
