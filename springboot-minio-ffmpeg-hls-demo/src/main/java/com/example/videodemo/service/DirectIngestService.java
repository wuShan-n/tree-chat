package com.example.videodemo.service;

import com.example.videodemo.controller.dto.IngestResponse;
import com.example.videodemo.service.dto.CompleteUploadCommand;
import com.example.videodemo.service.dto.CreateUploadCommand;
import com.example.videodemo.service.dto.IngestResult;
import com.example.videodemo.service.dto.UploadTicket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.file.Path;

@Service
public class DirectIngestService {

    private final VideoIngestService videoIngestService;
    private final String bucket;

    public DirectIngestService(VideoIngestService videoIngestService,
                               @Value("${app.s3.bucket}") String bucket) {
        this.videoIngestService = videoIngestService;
        this.bucket = bucket;
    }

    public IngestResponse ingest(String objectName) {
        Assert.hasText(objectName, "objectName must not be blank");

        Path path = Path.of(objectName);
        String fileName = path.getFileName().toString();

        UploadTicket ticket = videoIngestService.createUpload(new CreateUploadCommand(
                fileName,
                "auto generated ingest",
                fileName,
                0L,
                "LEGACY",
                "anonymous",
                bucket,
                objectName,
                "DIRECT",
                null));

        IngestResult result = videoIngestService.completeUpload(new CompleteUploadCommand(
                ticket.videoId(),
                ticket.uploadTaskId(),
                null,
                null,
                null));

        return toResponse(result);
    }

    public IngestResponse status(long videoId) {
        IngestResult result = videoIngestService.getStatus(videoId);
        return toResponse(result);
    }

    private IngestResponse toResponse(IngestResult result) {
        return new IngestResponse(
                result.videoId(),
                result.uploadTaskId(),
                result.transcodeJobId(),
                result.assetStatus() != null ? result.assetStatus().name() : null,
                result.jobStatus() != null ? result.jobStatus().name() : null,
                result.playbackUrl());
    }
}
