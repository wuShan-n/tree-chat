package com.example.videodemo.service;

import com.example.videodemo.service.dto.CompleteUploadCommand;
import com.example.videodemo.service.dto.CreateUploadCommand;
import com.example.videodemo.service.dto.IngestResult;
import com.example.videodemo.service.dto.UploadTicket;

public interface VideoIngestService {

    UploadTicket createUpload(CreateUploadCommand command);

    IngestResult completeUpload(CompleteUploadCommand command);

    IngestResult getStatus(long videoId);

    void markUploadFailed(long videoId, String reason);
}
