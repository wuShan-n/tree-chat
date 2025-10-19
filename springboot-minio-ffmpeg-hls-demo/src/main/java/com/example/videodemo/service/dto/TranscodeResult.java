package com.example.videodemo.service.dto;

import java.util.List;

public record TranscodeResult(String playbackUrl, List<VariantDescriptor> variants) {

    public record VariantDescriptor(int level,
                                    String resolution,
                                    int bitrateKbps,
                                    String playlistPath,
                                    String segmentPathPrefix,
                                    Integer durationSeconds,
                                    String checksum) {
    }
}

