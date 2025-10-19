package com.example.videodemo.service.dto;

import java.util.List;

public record PlaybackInfo(long videoId,
                           String assetStatus,
                           String playbackUrl,
                           List<Variant> variants) {

    public record Variant(int level,
                          String resolution,
                          Integer bitrateKbps,
                          String playlistPath,
                          String segmentPathPrefix,
                          Integer durationSeconds,
                          String checksum) {
    }
}

