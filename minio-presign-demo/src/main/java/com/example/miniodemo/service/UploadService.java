package com.example.miniodemo.service;

import com.example.miniodemo.dto.PresignReq;
import com.example.miniodemo.dto.PresignResp;
import com.example.miniodemo.entity.Image;
import com.example.miniodemo.mapper.ImageMapper;
import com.example.miniodemo.util.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final ImageMapper imageMapper;

    @Value("${app.bucket}")
    private String bucket;

    public PresignResp createPresignedPut(long ownerId, PresignReq req) {
        long id = Ids.newId();
        String key = "/image/%d/original".formatted(id);
        Image record = new Image();
        record.setId(id);
        record.setOwnerId(ownerId);
        record.setOriginalName(req.filename());
        record.setContentType(req.contentType());
        record.setStorageKey(key);
        record.setSizeBytes(req.size());
        record.setStatus("UPLOADING");
        imageMapper.insert(record);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        PutObjectPresignRequest preq = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(Duration.ofMinutes(15))
                .build();
        PresignedPutObjectRequest p = presigner.presignPutObject(preq);
        return new PresignResp(String.valueOf(id),p.url().toString(),Map.of("Content-Type",req.contentType()));

    }
}