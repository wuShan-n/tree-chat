package com.example.videodemo.web;

import com.example.videodemo.service.TranscodeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final TranscodeService transcodeService;

    public IngestController(TranscodeService transcodeService) {
        this.transcodeService = transcodeService;
    }

    // 客户端在直传完成后，调用这里触发转码
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, String> ingestForm(@RequestParam("objectName") String objectName) throws Exception {
        String play = transcodeService.transcodeToHls(objectName);
        return Map.of("playUrl", play);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> ingestJson(@RequestBody Map<String, String> body) throws Exception {
        String objectName = body.get("objectName");
        String play = transcodeService.transcodeToHls(objectName);
        return Map.of("playUrl", play);
    }
}