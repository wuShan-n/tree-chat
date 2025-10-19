package com.example.videodemo.controller;

import com.example.videodemo.controller.vo.PlaybackResponse;
import com.example.videodemo.service.VideoPlaybackService;
import com.example.videodemo.service.dto.PlaybackInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoPlaybackService playbackService;

    @GetMapping("/{videoId}/playback")
    public PlaybackResponse playback(@PathVariable("videoId") long videoId) {
        PlaybackInfo info = playbackService.getPlayback(videoId);
        return new PlaybackResponse(
                info.videoId(),
                info.assetStatus(),
                info.playbackUrl(),
                info.variants().stream()
                        .map(variant -> new PlaybackResponse.Variant(
                                variant.level(),
                                variant.resolution(),
                                variant.bitrateKbps(),
                                variant.playlistPath(),
                                variant.segmentPathPrefix(),
                                variant.durationSeconds(),
                                variant.checksum()))
                        .toList());
    }
}

