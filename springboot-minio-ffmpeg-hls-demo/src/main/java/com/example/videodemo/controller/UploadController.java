package com.example.videodemo.controller;

import com.example.videodemo.controller.dto.PresignGetResponse;
import com.example.videodemo.controller.dto.PresignUploadRequest;
import com.example.videodemo.controller.dto.PresignUploadResponse;
import com.example.videodemo.service.UploadPresignService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadPresignService uploadPresignService;

    @PostMapping(value = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PresignUploadResponse presignJson(@RequestBody PresignUploadRequest request) {
        return uploadPresignService.createUploadUrl(request);
    }

    @GetMapping("/presign-get")
    public PresignGetResponse presignDownload(@RequestParam("objectName") String objectName) {
        return uploadPresignService.createDownloadUrl(objectName);
    }
}
