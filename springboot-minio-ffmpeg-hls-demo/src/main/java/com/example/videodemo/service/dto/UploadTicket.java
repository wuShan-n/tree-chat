package com.example.videodemo.service.dto;

public record UploadTicket(
        long videoId,
        long uploadTaskId,
        String bucket,
        String objectKey) {
}
