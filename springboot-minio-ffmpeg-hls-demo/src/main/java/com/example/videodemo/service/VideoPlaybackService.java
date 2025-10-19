package com.example.videodemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.videodemo.persistence.entity.VideoAsset;
import com.example.videodemo.persistence.entity.VideoTranscodeVariant;
import com.example.videodemo.persistence.mapper.VideoAssetMapper;
import com.example.videodemo.persistence.mapper.VideoTranscodeVariantMapper;
import com.example.videodemo.service.dto.PlaybackInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoPlaybackService {

    private final VideoAssetMapper videoAssetMapper;
    private final VideoTranscodeVariantMapper variantMapper;

    public PlaybackInfo getPlayback(long videoId) {
        VideoAsset asset = Optional.ofNullable(videoAssetMapper.selectById(videoId))
                .orElseThrow(() -> new IllegalArgumentException("Video asset not found: " + videoId));

        List<VideoTranscodeVariant> variants = variantMapper.selectList(
                new LambdaQueryWrapper<VideoTranscodeVariant>()
                        .eq(VideoTranscodeVariant::getVideoId, videoId)
                        .orderByAsc(VideoTranscodeVariant::getVariantLevel));

        List<PlaybackInfo.Variant> variantDtos = variants.stream()
                .map(variant -> new PlaybackInfo.Variant(
                        Optional.ofNullable(variant.getVariantLevel()).orElse(0),
                        variant.getResolution(),
                        variant.getBitrateKbps(),
                        variant.getPlaylistPath(),
                        variant.getSegmentPathPrefix(),
                        variant.getDurationSeconds(),
                        variant.getChecksum()))
                .collect(Collectors.toList());

        return new PlaybackInfo(
                asset.getId(),
                asset.getStatus(),
                asset.getPlaybackUrl(),
                variantDtos);
    }
}

