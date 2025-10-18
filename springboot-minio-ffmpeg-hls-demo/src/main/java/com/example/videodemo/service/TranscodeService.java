package com.example.videodemo.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
public class TranscodeService {

    private final MinioClient minio;
    private final String bucket;
    private final Path vodRoot;
    private final String cdnPublicBase;
    private final String vodPrefix;

    public TranscodeService(MinioClient minio,
                            @Value("${app.minio.bucket}") String bucket,
                            @Value("${app.cdn.publicBase}") String cdnPublicBase,
                            @Value("${app.output.prefix}") String vodPrefix) {
        this.minio = minio;
        this.bucket = bucket;
        this.vodRoot = Path.of("storage/public/vod");
        this.cdnPublicBase = normalizeBase(cdnPublicBase);
        this.vodPrefix = ensureSuffix(vodPrefix, "/");
    }

    private static String guessContentType(Path f) {
        String n = f.getFileName().toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (n.endsWith(".m4s")) return "video/iso.segment";
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".ts")) return "video/mp2t";
        return "application/octet-stream";
    }

    private static String stripExt(String name) {
        int idx = name.lastIndexOf('.');
        return (idx > 0) ? name.substring(0, idx) : name;
    }

    private static String ensureSuffix(String s, String suf) {
        if (s == null || s.isEmpty()) return suf;
        return s.endsWith(suf) ? s : s + suf;
    }

    private static String normalizeBase(String base) {
        if (base == null || base.isEmpty()) return "/vod/";
        if (!base.endsWith("/")) base = base + "/";
        return base;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                System.out.println("Created bucket '" + bucket + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket", e);
        }
    }

    public String transcodeToHls(String objectName) throws Exception {

        Files.createDirectories(vodRoot);
        String baseName = stripExt(Path.of(objectName).getFileName().toString());
        Path outDir = vodRoot.resolve(baseName);
        if (!Files.exists(outDir)) Files.createDirectories(outDir);

        // 1) 下载源文件到临时盘
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        String tempFileName = "input-" + UUID.randomUUID() + ".mp4";
        Path tmp = tempDir.resolve(tempFileName);


        try {
            minio.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucket).object(objectName)
                    .filename(tmp.toAbsolutePath().toString())
                    .build());

            // 构造 ffmpeg 命令：三档(1080/720/480) + CMAF HLS (fMP4)
            List<String> cmd = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-i", tmp.toAbsolutePath().toString(),

                    // 三组(视频+音频)一一对应
                    "-map", "0:v:0", "-map", "0:a:0?",
                    "-map", "0:v:0", "-map", "0:a:0?",
                    "-map", "0:v:0", "-map", "0:a:0?",

                    // 三档视频
                    "-c:v:0", "libx264", "-preset", "veryfast", "-crf", "22",
                    "-filter:v:0", "scale=-2:1080",
                    "-b:v:0", "5000k", "-maxrate:v:0", "5350k", "-bufsize:v:0", "7500k",

                    "-c:v:1", "libx264", "-preset", "veryfast", "-crf", "24",
                    "-filter:v:1", "scale=-2:720",
                    "-b:v:1", "2800k", "-maxrate:v:1", "2996k", "-bufsize:v:1", "4200k",

                    "-c:v:2", "libx264", "-preset", "veryfast", "-crf", "26",
                    "-filter:v:2", "scale=-2:480",
                    "-b:v:2", "1400k", "-maxrate:v:2", "1498k", "-bufsize:v:2", "2100k",

                    // 三条音频（避免复用同一条 a:0）
                    "-c:a:0", "aac", "-ac:0", "2", "-b:a:0", "128k",
                    "-c:a:1", "aac", "-ac:1", "2", "-b:a:1", "128k",
                    "-c:a:2", "aac", "-ac:2", "2", "-b:a:2", "128k",

                    // 帧率/GOP
                    "-r", "30", "-g", "60", "-keyint_min", "60", "-sc_threshold", "0",

                    // 推荐去掉元数据/章节以免引发额外流
                    "-map_metadata", "-1", "-map_chapters", "-1",

                    // HLS (CMAF fMP4)
                    "-f", "hls",
                    "-hls_time", "4",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_type", "fmp4",
                    "-hls_flags", "independent_segments+split_by_time",
                    "-master_pl_name", "master.m3u8",

                    // 一一对应：v0↔a0, v1↔a1, v2↔a2
                    "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",

                    "-hls_segment_filename", outDir.resolve("v%v/seg_%06d.m4s").toString().replace('\\', '/'),
                    outDir.resolve("v%v/stream.m3u8").toString().replace('\\', '/')
            ));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("ffmpeg failed, exit code=" + code);
            }

            // 3) 上传 HLS 输出目录到 MinIO（保持目录结构）
            String keyPrefix = vodPrefix + baseName + "/";
            uploadDirToMinio(outDir, keyPrefix);

            // 4) 返回 CDN 播放地址
            return cdnPublicBase + baseName + "/master.m3u8";
        } finally {
            try {
                Files.deleteIfExists(tmp); // 清理临时输入文件
                deleteDirectory(outDir);   // **(新) 清理临时输出目录**
            } catch (IOException e) {
                System.err.println("Failed to cleanup temp files: " + e.getMessage());
                // 在生产中，这里应该记日志，而不是打印到控制台
            }
        }
    }

    private void uploadDirToMinio(Path localDir, String keyPrefix) throws Exception {
        List<Path> files = Files.walk(localDir)
                .filter(Files::isRegularFile)
                .sorted()
                .toList();

        for (Path f : files) {
            String rel = localDir.relativize(f).toString().replace("\\", "/");
            String key = keyPrefix + rel;
            String contentType = guessContentType(f);
            long size = Files.size(f);

            try (InputStream is = Files.newInputStream(f)) {
                PutObjectArgs args = PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .contentType(contentType)
                        .stream(is, size, -1)
                        .build();
                minio.putObject(args);
                System.out.println("Uploaded: " + key + " (" + contentType + ")");
            }
        }
    }
    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}