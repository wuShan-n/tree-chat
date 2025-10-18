package com.example.miniodemo.controller;

import com.example.miniodemo.dto.PresignReq;
import com.example.miniodemo.dto.PresignResp;
import com.example.miniodemo.service.ProcessingService;
import com.example.miniodemo.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;
    private final ProcessingService processingService;

    // 模拟登录用户：演示用固定 ownerId=1
    @PostMapping("/uploads/presign")
    public PresignResp presign(@Valid @RequestBody PresignReq req) {
        return uploadService.createPresignedPut(1L, req);
    }

    @PostMapping("/uploads/{id}/complete")
    public void complete(@PathVariable long id) throws IOException {
        processingService.verifyAndProcess(id);
    }
}