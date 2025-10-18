package com.example.miniodemo.service;

import com.example.miniodemo.entity.Image;
import com.example.miniodemo.entity.ImageVariant;
import com.example.miniodemo.mapper.ImageMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ProcessingService {
    private final S3Client s3;
    private final ImageMapper imageMapper;
    private final Tika tika = new Tika();
    @Value("${app.bucket}") String bucket;

    @PostConstruct
    public void init() {
        // 1. 强制 ImageIO 重新扫描所有类路径上的插件
        ImageIO.scanForPlugins();

        // 3. 再次确认
        String[] formats = ImageIO.getWriterFormatNames();
        System.out.println("Available ImageIO Writer Formats after explicit load: " + Arrays.toString(formats));
    }

    @Async
    public void verifyAndProcess(long id) throws IOException {

        Image img = imageMapper.findById(id);
        if (img == null) return;

        try (ResponseInputStream<?> in = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(img.getStorageKey()).build())) {

            // 1) MIME 嗅探
            String mime = tika.detect(in);
            if (mime == null || !mime.startsWith("image/")) throw new IllegalArgumentException("Not an image");

            // 需要重新获取流（上面 detect 消耗了流），再读入 BufferedImage
        }

        try (ResponseInputStream<?> in2 = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(img.getStorageKey()).build())) {
            BufferedImage bi = ImageIO.read(in2);
            int w = bi.getWidth(), h = bi.getHeight();

            // 2) 生成 1024 宽 WebP 变体
            var resized = new java.io.ByteArrayOutputStream();
            // 用 Thumbnailator 简化缩放
            net.coobird.thumbnailator.Thumbnails.of(bi)
                    .size(1024, 1024)
                    .outputFormat("webp")
                    .toOutputStream(resized);
            byte[] bytes = resized.toByteArray();
            String vKey = img.getStorageKey().replace("/original", "/w1024.webp");

            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(vKey)
                            .contentType("image/webp").build(),
                    RequestBody.fromBytes(bytes));

            // 3) 更新 DB
            img.setStatus("READY");
            img.setWidth(w); img.setHeight(h);
            imageMapper.updateAfterProcess(img);

            ImageVariant v = new ImageVariant();
            v.setId(System.nanoTime());
            v.setImageId(id);
            v.setVariant("w1024");
            v.setFormat("webp");
            v.setStorageKey(vKey);
            v.setSizeBytes((long) bytes.length);
            imageMapper.insertVariant(v);
        } catch (Exception e) {
            Image fail = new Image();
            fail.setId(id);
            fail.setStatus("FAILED");
            imageMapper.updateAfterProcess(fail);
            throw new RuntimeException(e);
        }
    }
}