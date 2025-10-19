package com.example.videodemo.controller;

import com.example.videodemo.controller.dto.IngestRequest;
import com.example.videodemo.controller.dto.IngestResponse;
import com.example.videodemo.service.DirectIngestService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final DirectIngestService directIngestService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngestResponse ingestJson(@RequestBody IngestRequest request) {
        return directIngestService.ingest(request.objectName());
    }

    @GetMapping("/{videoId}")
    public IngestResponse status(@PathVariable("videoId") long videoId) {
        return directIngestService.status(videoId);
    }
}
