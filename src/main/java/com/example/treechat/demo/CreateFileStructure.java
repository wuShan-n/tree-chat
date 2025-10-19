package com.example.treechat.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CreateFileStructure {

    public static void main(String[] args) {
        String baseDir = "D:\\file\\python\\java\\ai\\tree-chat\\vod-sample";

        // 定义所有需要创建的文件路径
        List<String> files = Arrays.asList(
            "docker-compose.yml",
            "README.md",
            "sql/schema.sql",
            "video-service/Dockerfile",
            "video-service/pom.xml",
            "video-service/src/main/java/com/example/vod/VideoServiceApplication.java",
            "video-service/src/main/java/com/example/vod/config/KafkaConfig.java",
            "video-service/src/main/java/com/example/vod/config/MinioConfig.java",
            "video-service/src/main/java/com/example/vod/controller/AssetController.java",
            "video-service/src/main/java/com/example/vod/controller/UploadController.java",
            "video-service/src/main/java/com/example/vod/controller/PlaybackController.java",
            "video-service/src/main/java/com/example/vod/domain/Asset.java",
            "video-service/src/main/java/com/example/vod/domain/AssetStatus.java",
            "video-service/src/main/java/com/example/vod/repo/AssetRepository.java",
            "video-service/src/main/java/com/example/vod/service/AssetService.java",
            "video-service/src/main/java/com/example/vod/service/JobPublisher.java",
            "video-service/src/main/java/com/example/vod/service/PlaybackService.java",
            "video-service/src/main/java/com/example/vod/dto/CreateAssetReq.java",
            "video-service/src/main/java/com/example/vod/dto/CreateAssetResp.java",
            "video-service/src/main/java/com/example/vod/dto/PresignPartReq.java",
            "video-service/src/main/java/com/example/vod/dto/PresignPartResp.java",
            "video-service/src/main/java/com/example/vod/dto/JobTranscodeMsg.java",
            "video-service/src/main/java/com/example/vod/dto/WorkerCallbackReq.java",
            "media-worker/Dockerfile",
            "media-worker/pom.xml",
            "media-worker/src/main/java/com/example/worker/MediaWorkerApplication.java",
            "media-worker/src/main/java/com/example/worker/config/KafkaConfig.java",
            "media-worker/src/main/java/com/example/worker/config/MinioConfig.java",
            "media-worker/src/main/java/com/example/worker/consumer/JobConsumer.java",
            "media-worker/src/main/java/com/example/worker/ffmpeg/FfprobeService.java",
            "media-worker/src/main/java/com/example/worker/ffmpeg/FfmpegHlsService.java",
            "media-worker/src/main/java/com/example/worker/ffmpeg/CommandRunner.java",
            "media-worker/src/main/java/com/example/worker/minio/MinioDownloader.java",
            "media-worker/src/main/java/com/example/worker/minio/MinioUploader.java",
            "media-worker/src/main/java/com/example/worker/dto/JobTranscodeMsg.java"
        );

        try {
            // 创建根目录
            Path root = Paths.get(baseDir);
            Files.createDirectories(root);

            // 遍历文件列表，创建父目录和空文件
            for (String file : files) {
                Path filePath = root.resolve(file);
                // 确保父目录存在
                Files.createDirectories(filePath.getParent());
                // 创建文件
                Files.createFile(filePath);
            }

            System.out.println("目录结构创建成功: " + root.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("创建目录结构时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}