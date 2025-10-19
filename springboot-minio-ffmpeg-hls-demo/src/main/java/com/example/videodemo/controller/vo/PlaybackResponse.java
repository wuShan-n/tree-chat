package com.example.videodemo.controller.vo;

import java.util.List;

public record PlaybackResponse(long videoId,
                               String status,
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

