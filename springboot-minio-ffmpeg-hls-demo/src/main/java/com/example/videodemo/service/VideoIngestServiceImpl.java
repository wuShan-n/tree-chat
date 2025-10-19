package com.example.videodemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.videodemo.persistence.entity.*;
import com.example.videodemo.persistence.mapper.VideoAssetMapper;
import com.example.videodemo.persistence.mapper.VideoTranscodeJobMapper;
import com.example.videodemo.persistence.mapper.VideoUploadTaskMapper;
import com.example.videodemo.service.dto.CompleteUploadCommand;
import com.example.videodemo.service.dto.CreateUploadCommand;
import com.example.videodemo.service.dto.IngestResult;
import com.example.videodemo.service.dto.UploadTicket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoIngestServiceImpl implements VideoIngestService {

    private static final String DEFAULT_JOB_TYPE = "INGEST";
    private static final String DEFAULT_TRANSCODE_PROFILE = "default-hls";

    private final VideoAssetMapper videoAssetMapper;
    private final VideoUploadTaskMapper uploadTaskMapper;
    private final VideoTranscodeJobMapper transcodeJobMapper;
    private final VideoTranscodeWorkflowService transcodeWorkflowService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public UploadTicket createUpload(CreateUploadCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        UploadTicket ticket = transactionTemplate.execute(status -> {
            VideoAsset toInsert = buildVideoAsset(command);
            videoAssetMapper.insert(toInsert);

            VideoUploadTask uploadTask = buildUploadTask(command, toInsert.getId());
            uploadTaskMapper.insert(uploadTask);

            return new UploadTicket(
                    toInsert.getId(),
                    uploadTask.getId(),
                    toInsert.getSourceBucket(),
                    toInsert.getSourceObject());
        });

        if (ticket == null) {
            throw new IllegalStateException("Failed to create upload ticket");
        }
        return ticket;
    }

    @Override
    public IngestResult completeUpload(CompleteUploadCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ProcessingContext context = transactionTemplate.execute(status -> {
            VideoAsset asset = videoAssetMapper.selectById(command.videoId());
            if (asset == null) {
                throw new IllegalArgumentException("Video asset not found: " + command.videoId());
            }

            if (command.durationSeconds() != null) {
                asset.setDurationSeconds(command.durationSeconds());
            }
            if (command.checksum() != null && !command.checksum().isBlank()) {
                asset.setChecksum(command.checksum());
            }
            if (command.transcodeProfile() != null) {
                asset.setTranscodeProfile(command.transcodeProfile());
            }
            asset.setStatus(VideoAssetStatus.PROCESSING.name());
            videoAssetMapper.updateById(asset);

            Long uploadTaskId = null;
            if (command.uploadTaskId() != null) {
                VideoUploadTask task = uploadTaskMapper.selectById(command.uploadTaskId());
                if (task != null) {
                    task.setProgress(100.0);
                    task.setValidationStatus("PASSED");
                    uploadTaskMapper.updateById(task);
                    uploadTaskId = task.getId();
                }
            }

            VideoTranscodeJob transcodeJob = VideoTranscodeJob.builder()
                    .videoId(asset.getId())
                    .jobType(DEFAULT_JOB_TYPE)
                    .targetProfile(asset.getTranscodeProfile())
                    .priority(5)
                    .status(TranscodeJobStatus.PENDING.name())
                    .build();
            transcodeJobMapper.insert(transcodeJob);

            return new ProcessingContext(
                    asset.getId(),
                    uploadTaskId,
                    transcodeJob.getId(),
                    VideoAssetStatus.valueOf(asset.getStatus()),
                    TranscodeJobStatus.PENDING);
        });

        if (context == null) {
            throw new IllegalStateException("Failed to create transcode job");
        }

        transcodeWorkflowService.dispatch(context.jobId());

        return new IngestResult(
                context.videoId(),
                context.uploadTaskId(),
                context.jobId(),
                context.assetStatus(),
                context.jobStatus(),
                null);
    }

    @Override
    public IngestResult getStatus(long videoId) {
        VideoAsset asset = videoAssetMapper.selectById(videoId);
        if (asset == null) {
            throw new IllegalArgumentException("Video asset not found: " + videoId);
        }

        VideoUploadTask uploadTask = uploadTaskMapper.selectOne(
                new LambdaQueryWrapper<VideoUploadTask>()
                        .eq(VideoUploadTask::getVideoId, videoId)
                        .orderByDesc(VideoUploadTask::getCreatedAt)
                        .last("limit 1"));

        VideoTranscodeJob job = transcodeJobMapper.selectOne(
                new LambdaQueryWrapper<VideoTranscodeJob>()
                        .eq(VideoTranscodeJob::getVideoId, videoId)
                        .orderByDesc(VideoTranscodeJob::getCreatedAt)
                        .last("limit 1"));

        VideoAssetStatus assetStatus = VideoAssetStatus.valueOf(asset.getStatus());
        TranscodeJobStatus jobStatus = (job != null && job.getStatus() != null)
                ? TranscodeJobStatus.valueOf(job.getStatus())
                : null;

        return new IngestResult(
                asset.getId(),
                uploadTask != null ? uploadTask.getId() : null,
                job != null ? job.getId() : null,
                assetStatus,
                jobStatus,
                asset.getPlaybackUrl());
    }

    @Override
    public void markUploadFailed(long videoId, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            VideoAsset asset = videoAssetMapper.selectById(videoId);
            if (asset != null) {
                asset.setStatus(VideoAssetStatus.FAILED.name());
                videoAssetMapper.updateById(asset);
            }
            VideoUploadTask task = uploadTaskMapper.selectOne(
                    new LambdaQueryWrapper<VideoUploadTask>()
                            .eq(VideoUploadTask::getVideoId, videoId)
                            .orderByDesc(VideoUploadTask::getCreatedAt)
                            .last("limit 1"));
            if (task != null) {
                task.setValidationStatus("FAILED");
                task.setRemark(reason);
                uploadTaskMapper.updateById(task);
            }
        });
    }

    private VideoAsset buildVideoAsset(CreateUploadCommand command) {
        return VideoAsset.builder()
                .title(command.title())
                .description(command.description())
                .fileName(command.fileName())
                .fileSize(command.fileSize())
                .checksum(command.checksum())
                .sourceBucket(command.sourceBucket())
                .sourceObject(command.sourceObject())
                .status(VideoAssetStatus.UPLOADING.name())
                .transcodeProfile(DEFAULT_TRANSCODE_PROFILE)
                .build();
    }

    private VideoUploadTask buildUploadTask(CreateUploadCommand command, Long videoId) {
        return VideoUploadTask.builder()
                .videoId(videoId)
                .uploadChannel(command.uploadChannel())
                .uploaderId(command.uploaderId())
                .uploadPath(command.sourceObject())
                .ingestStrategy(command.ingestStrategy())
                .progress(0.0)
                .checksum(command.checksum())
                .validationStatus("PENDING")
                .build();
    }

    private record ProcessingContext(long videoId,
                                     Long uploadTaskId,
                                     Long jobId,
                                     VideoAssetStatus assetStatus,
                                     TranscodeJobStatus jobStatus) {
    }
}
