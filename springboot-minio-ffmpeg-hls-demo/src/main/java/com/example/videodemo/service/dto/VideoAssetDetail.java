package com.example.videodemo.service.dto;

import com.example.videodemo.persistence.entity.VideoAssetStatus;

import java.time.LocalDateTime;

public record VideoAssetDetail(
        long videoId,
        String title,
        String description,
        String sourceObject,
        VideoAssetStatus status,
        String playbackUrl,
        LocalDateTime readyAt) {
}
