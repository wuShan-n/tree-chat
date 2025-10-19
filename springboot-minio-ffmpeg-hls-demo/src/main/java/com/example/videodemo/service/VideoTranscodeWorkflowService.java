package com.example.videodemo.service;

import com.example.videodemo.persistence.entity.TranscodeJobStatus;
import com.example.videodemo.persistence.entity.VideoAsset;
import com.example.videodemo.persistence.entity.VideoAssetStatus;
import com.example.videodemo.persistence.entity.VideoTranscodeJob;
import com.example.videodemo.persistence.mapper.VideoAssetMapper;
import com.example.videodemo.persistence.mapper.VideoTranscodeJobMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class VideoTranscodeWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodeWorkflowService.class);

    private final VideoTranscodeJobMapper transcodeJobMapper;
    private final VideoAssetMapper videoAssetMapper;
    private final TranscodeService transcodeService;
    private final TransactionTemplate transactionTemplate;
    @Qualifier("transcodeExecutor")
    private final TaskExecutor transcodeExecutor;

    public void dispatch(Long jobId) {
        if (Boolean.FALSE.equals(markDispatched(jobId))) {
            log.warn("Transcode job {} could not be marked as dispatched.", jobId);
            return;
        }
        transcodeExecutor.execute(() -> runJob(jobId));
    }

    private void runJob(Long jobId) {
        VideoJobContext context = prepareJob(jobId);
        if (context == null) {
            log.warn("Transcode job {} skipped because preparation failed.", jobId);
            return;
        }

        try {
            String playbackUrl = transcodeService.transcodeToHls(context.asset().getSourceObject());
            markSuccess(jobId, playbackUrl);
        } catch (Exception ex) {
            log.error("Transcode job {} failed.", jobId, ex);
            markFailure(jobId, ex.getMessage());
        }
    }

    private VideoJobContext prepareJob(Long jobId) {
        return transactionTemplate.execute(status -> {
            VideoTranscodeJob job = transcodeJobMapper.selectById(jobId);
            if (job == null) {
                log.warn("Transcode job {} not found during preparation.", jobId);
                return null;
            }

            VideoAsset asset = videoAssetMapper.selectById(job.getVideoId());
            if (asset == null) {
                log.error("Video asset {} missing for transcode job {}", job.getVideoId(), jobId);
                job.setStatus(TranscodeJobStatus.FAILED.name());
                job.setFinishedAt(LocalDateTime.now());
                job.setErrorMessage("Missing video asset");
                transcodeJobMapper.updateById(job);
                return null;
            }

            job.setStatus(TranscodeJobStatus.RUNNING.name());
            job.setStartedAt(LocalDateTime.now());
            transcodeJobMapper.updateById(job);

            return new VideoJobContext(job, asset);
        });
    }

    private Boolean markDispatched(Long jobId) {
        return transactionTemplate.execute(status -> {
            VideoTranscodeJob job = transcodeJobMapper.selectById(jobId);
            if (job == null) {
                return Boolean.FALSE;
            }
            if (!TranscodeJobStatus.PENDING.name().equals(job.getStatus())) {
                return Boolean.TRUE;
            }
            job.setStatus(TranscodeJobStatus.DISPATCHED.name());
            transcodeJobMapper.updateById(job);
            return Boolean.TRUE;
        });
    }

    private void markSuccess(Long jobId, String playbackUrl) {
        transactionTemplate.executeWithoutResult(status -> {
            VideoTranscodeJob job = Optional.ofNullable(transcodeJobMapper.selectById(jobId))
                    .orElseThrow(() -> new IllegalStateException("Transcode job not found during success handling: " + jobId));

            job.setStatus(TranscodeJobStatus.SUCCESS.name());
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            transcodeJobMapper.updateById(job);

            VideoAsset asset = Optional.ofNullable(videoAssetMapper.selectById(job.getVideoId()))
                    .orElseThrow(() -> new IllegalStateException("Video asset not found during success handling: " + job.getVideoId()));
            asset.setStatus(VideoAssetStatus.READY.name());
            asset.setReadyAt(LocalDateTime.now());
            asset.setPlaybackUrl(playbackUrl);
            videoAssetMapper.updateById(asset);
        });
    }

    private void markFailure(Long jobId, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            VideoTranscodeJob job = transcodeJobMapper.selectById(jobId);
            if (job != null) {
                job.setStatus(TranscodeJobStatus.FAILED.name());
                job.setFinishedAt(LocalDateTime.now());
                job.setErrorMessage(reason);
                transcodeJobMapper.updateById(job);
            }

            if (job != null) {
                VideoAsset asset = videoAssetMapper.selectById(job.getVideoId());
                if (asset != null) {
                    asset.setStatus(VideoAssetStatus.FAILED.name());
                    videoAssetMapper.updateById(asset);
                }
            }
        });
    }

    private record VideoJobContext(VideoTranscodeJob job, VideoAsset asset) {
    }
}
