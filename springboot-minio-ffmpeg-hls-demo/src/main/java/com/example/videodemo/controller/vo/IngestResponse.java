package com.example.videodemo.controller.vo;

public record IngestResponse(
        long videoId,
        Long uploadTaskId,
        long transcodeJobId,
        String assetStatus,
        String jobStatus,
        String playbackUrl) {
}
