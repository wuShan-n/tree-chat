package com.example.videodemo.service.dto;

import com.example.videodemo.persistence.entity.TranscodeJobStatus;
import com.example.videodemo.persistence.entity.VideoAssetStatus;

public record IngestResult(
        long videoId,
        Long uploadTaskId,
        Long transcodeJobId,
        VideoAssetStatus assetStatus,
        TranscodeJobStatus jobStatus,
        String playbackUrl) {
}
