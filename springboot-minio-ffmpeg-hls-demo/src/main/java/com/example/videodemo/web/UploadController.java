package com.example.videodemo.web;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final MinioClient minio;
    private final String bucket;

    public UploadController(MinioClient minio, @Value("${app.minio.bucket}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    @PostMapping(value = "/presign", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, String> presignForm(@RequestParam("objectName") String objectName,
                                           @RequestParam(value = "contentType", required = false, defaultValue = "video/mp4") String contentType) throws Exception {
        return presign(objectName, contentType);
    }

    @PostMapping(value = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> presignJson(@RequestBody Map<String, String> body) throws Exception {
        String objectName = body.getOrDefault("objectName", "video-" + System.currentTimeMillis() + ".mp4");
        String contentType = body.getOrDefault("contentType", "video/mp4");
        return presign(objectName, contentType);
    }

    private Map<String, String> presign(String objectName, String contentType) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("Content-Type", contentType);
        String url = minio.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry((int) Duration.ofMinutes(15).getSeconds())
                        .extraQueryParams(params)
                        .build()
        );
        Map<String, String> resp = new HashMap<>();
        resp.put("url", url);
        resp.put("bucket", bucket);
        resp.put("objectName", objectName);
        return resp;
    }

    @GetMapping("/presign-get")
    public Map<String, String> presignGet(@RequestParam("objectName") String objectName) throws Exception {
        String url = minio.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(60 * 10)
                        .build()
        );
        return Map.of("url", url);
    }
}