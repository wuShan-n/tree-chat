package com.example.videodemo.service.dto;

public record CompleteUploadCommand(
        long videoId,
        Long uploadTaskId,
        String checksum,
        Integer durationSeconds,
        String transcodeProfile) {
}
