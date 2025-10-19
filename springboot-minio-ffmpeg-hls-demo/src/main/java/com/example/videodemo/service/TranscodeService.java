package com.example.videodemo.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class TranscodeService {

    private static final Logger log = LoggerFactory.getLogger(TranscodeService.class);
    private static final List<TranscodeVariant> VARIANTS = List.of(
            new TranscodeVariant(1080, 5000, 5350, 7500, 22)
//            new TranscodeVariant(720, 2800, 2996, 4200, 24),
//            new TranscodeVariant(480, 1400, 1498, 2100, 26)
    );
    private static final String AUDIO_BITRATE = "128k";
    private static final String AUDIO_CHANNELS = "2";
    private static final String WORK_DIR_PREFIX = "hls-";

    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final Path vodRoot;
    private final String cdnPublicBase;
    private final String vodPrefix;

    public TranscodeService(S3Client s3Client,
                            @Value("${app.s3.bucket}") String bucket,
                            @Value("${app.s3.region}") String region,
                            @Value("${app.cdn.publicBase}") String cdnPublicBase,
                            @Value("${app.output.prefix}") String vodPrefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
        this.vodRoot = Path.of("storage/public/vod");
        this.cdnPublicBase = normalizeBase(cdnPublicBase);
        this.vodPrefix = ensureSuffix(vodPrefix, "/");
    }

    private static String guessContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (name.endsWith(".m4s")) {
            return "video/iso.segment";
        }
        if (name.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (name.endsWith(".ts")) {
            return "video/mp2t";
        }
        return "application/octet-stream";
    }

    private static String stripExt(String name) {
        int idx = name.lastIndexOf('.');
        return (idx > 0) ? name.substring(0, idx) : name;
    }

    private static String ensureSuffix(String value, String suffix) {
        if (value == null || value.isEmpty()) {
            return suffix;
        }
        return value.endsWith(suffix) ? value : value + suffix;
    }

    private static String normalizeBase(String base) {
        if (base == null || base.isEmpty()) {
            return "/vod/";
        }
        return base.endsWith("/") ? base : base + "/";
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException noBucket) {
            createBucket();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                createBucket();
            } else {
                throw new RuntimeException("Failed to access S3 bucket %s".formatted(bucket), ex);
            }
        }
    }

    private void createBucket() {
        CreateBucketRequest.Builder builder = CreateBucketRequest.builder().bucket(bucket);
        if (!"us-east-1".equalsIgnoreCase(region)) {
            builder = builder.createBucketConfiguration(
                    CreateBucketConfiguration.builder()
                            .locationConstraint(region)
                            .build());
        }
        try {
            s3Client.createBucket(builder.build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 409) {
                throw new RuntimeException("Failed to create S3 bucket %s".formatted(bucket), ex);
            }
        }
    }

    public String transcodeToHls(String objectKey) throws Exception {
        Files.createDirectories(vodRoot);
        String baseName = stripExt(Path.of(objectKey).getFileName().toString());
        Path workingDir = Files.createTempDirectory(vodRoot, WORK_DIR_PREFIX + baseName + "-");
        Path outputDir = workingDir.resolve("hls");
        Files.createDirectories(outputDir);
        Path inputFile = Files.createTempFile(workingDir, "input-", ".mp4");

        try {
            downloadSourceObject(objectKey, inputFile);
            prepareVariantDirectories(outputDir);

            List<String> ffmpegCommand = buildFfmpegCommand(inputFile, outputDir);
            try {
                runFfmpeg(ffmpegCommand);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("ffmpeg execution interrupted", ex);
            }

            String keyPrefix = vodPrefix + baseName + "/";
            uploadDirToS3(outputDir, keyPrefix);
            return cdnPublicBase + baseName + "/master.m3u8";
        } finally {
            deleteDirectory(workingDir);
        }
    }

    private void downloadSourceObject(String objectKey, Path destination) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
             OutputStream out = Files.newOutputStream(destination)) {
            response.transferTo(out);
        } catch (SdkException ex) {
            throw new IOException("Failed to download S3 object %s".formatted(objectKey), ex);
        }
    }

    private void prepareVariantDirectories(Path outputDir) throws IOException {
        for (int i = 0; i < VARIANTS.size(); i++) {
            Files.createDirectories(outputDir.resolve("v" + i));
        }
    }

    private List<String> buildFfmpegCommand(Path input, Path outDir) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(input.toAbsolutePath().toString());

        for (int i = 0; i < VARIANTS.size(); i++) {
            command.add("-map");
            command.add("0:v:0");
            command.add("-map");
            command.add("0:a:0?");
        }

        for (int i = 0; i < VARIANTS.size(); i++) {
            TranscodeVariant variant = VARIANTS.get(i);
            command.add("-c:v:" + i);
            command.add("libx264");
            command.add("-preset");
            command.add("veryfast");
            command.add("-crf");
            command.add(Integer.toString(variant.crf()));
            command.add("-filter:v:" + i);
            command.add("scale=-2:" + variant.height());
            command.add("-b:v:" + i);
            command.add(variant.videoBitrate());
            command.add("-maxrate:v:" + i);
            command.add(variant.maxRate());
            command.add("-bufsize:v:" + i);
            command.add(variant.bufferSize());

            command.add("-c:a:" + i);
            command.add("aac");
            command.add("-ac:" + i);
            command.add(AUDIO_CHANNELS);
            command.add("-b:a:" + i);
            command.add(AUDIO_BITRATE);
        }

        command.addAll(List.of(
                "-r", "30",
                "-g", "60",
                "-keyint_min", "60",
                "-sc_threshold", "0",
                "-map_metadata", "-1",
                "-map_chapters", "-1",
                "-f", "hls",
                "-hls_time", "4",
                "-hls_playlist_type", "vod",
                "-hls_segment_type", "fmp4",
                "-hls_flags", "independent_segments+split_by_time",
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", buildVarStreamMap(),
                "-hls_segment_filename", outDir.resolve("v%v/seg_%06d.m4s").toString().replace('\\', '/'),
                outDir.resolve("v%v/stream.m3u8").toString().replace('\\', '/')
        ));
        return command;
    }

    private String buildVarStreamMap() {
        return IntStream.range(0, VARIANTS.size())
                .mapToObj(i -> "v:%d,a:%d".formatted(i, i))
                .collect(Collectors.joining(" "));
    }

    private void runFfmpeg(List<String> command) throws IOException, InterruptedException {
        log.info("Invoking ffmpeg with {} arguments", command.size());
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[ffmpeg] {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("ffmpeg failed, exit code=" + exitCode);
        }
    }

    private void uploadDirToS3(Path localDir, String keyPrefix) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(localDir)) {
            files = walk.filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }

        for (Path file : files) {
            String rel = localDir.relativize(file).toString().replace("\\", "/");
            String key = keyPrefix + rel;
            String contentType = guessContentType(file);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            try {
                s3Client.putObject(request, RequestBody.fromFile(file));
                log.info("Uploaded {} ({}).", key, contentType);
            } catch (SdkException ex) {
                throw new IOException("Failed to upload HLS artifact to S3 key %s".formatted(key), ex);
            }
        }
    }

    private void deleteDirectory(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ex) {
                    log.warn("Failed to delete temp path {}", p, ex);
                }
            });
        } catch (IOException ex) {
            log.warn("Failed to cleanup working directory {}", path, ex);
        }
    }

    private record TranscodeVariant(int height, int videoBitrateKbps, int maxBitrateKbps, int bufferSizeKbps, int crf) {
        String videoBitrate() {
            return videoBitrateKbps + "k";
        }

        String maxRate() {
            return maxBitrateKbps + "k";
        }

        String bufferSize() {
            return bufferSizeKbps + "k";
        }
    }
}
