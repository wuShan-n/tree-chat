
# Spring Boot + MinIO 直传 + FFmpeg → HLS Demo

一个最小可跑通的端到端示例：
- 前端页面选择视频 → 请求 **预签名 PUT** → 直传到 MinIO
- 前端调用 `/api/ingest` 通知后端
- 后端调用 **FFmpeg** 将源视频转三档 HLS (CMAF fMP4)
- HLS 输出落地本地目录 `storage/public/vod/<name>/master.m3u8`，由 Spring 静态映射 `/vod/**` 提供
- 页面用 hls.js 播放。

> 仅用于快速 POC。生产请加入鉴权、作业队列、失败重试、CDN、DRM 等。

## 1. 前置
- 安装 **Java 17+**
- 安装 **FFmpeg**（`ffmpeg -version` 能运行）
- 安装 **Docker**（用于启动 MinIO）

## 2. 启动 MinIO
```bash
docker compose up -d
# 控制台: http://localhost:9001 (账号/密码: minioadmin/minioadmin)
```
进入 MinIO 控制台，新建 **同名 bucket: `videos`**（应用启动会自动创建，但建议确认）。

### 设置 CORS（允许浏览器直传）
在 MinIO Console 打开 `videos` -> Settings -> CORS，粘贴：
```json
[
  {
    "AllowedOrigins": [
      "http://localhost:8080"
    ],
    "AllowedMethods": [
      "PUT",
      "GET"
    ],
    "AllowedHeaders": [
      "*"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3000
  }
]
```

> 若使用 `mc` 客户端，也可：
> ```bash
> docker run --rm --network=host -v "$PWD/scripts:/scripts" minio/mc >   sh -c "mc alias set local http://localhost:9000 minioadmin minioadmin && mc mb --ignore-existing local/videos && mc anonymous set none local/videos && mc set cors local/videos /scripts/minio-cors.json"
> ```

## 3. 启动应用
```bash
./gradlew bootRun
# 浏览器打开: http://localhost:8080
```

## 4. 测试流程
1. 选择一个小视频（mp4/mov 等），点击“上传并转码”。
2. 观察日志：预签名 → PUT 到 MinIO → 触发 `/api/ingest` → FFmpeg 输出。
3. 完成后页面自动开始播放：`/vod/<文件名去扩展名>/master.m3u8`。

## 5. 常见问题
- **CORS 报错**：确认已为 `videos` 桶设置允许 `http://localhost:8080` 的 PUT/GET。
- **ffmpeg 未找到**：`brew install ffmpeg`（macOS），`choco install ffmpeg`（Windows），或 Linux 包管理器安装。
- **HLS 播放黑屏**：检查 `storage/public/vod/<name>/` 下是否生成 `master.m3u8` 和 `v0/v1/v2` 子目录的分片。
- **码率过高/转码慢**：在 `TranscodeService` 调整 CRF、b:v、preset。生产建议 GPU/NVENC。

## 6. 结构
```
.
├── docker-compose.yml
├── scripts/minio-cors.json
├── src/main/java/com/example/videodemo
│   ├── DemoApplication.java
│   ├── config
│   │   ├── MinioConfig.java
│   │   └── WebConfig.java
│   ├── service
│   │   └── TranscodeService.java
│   └── web
│       ├── IngestController.java
│       └── UploadController.java
├── src/main/resources
│   ├── application.yml
│   └── static/index.html
├── storage/public/vod/   # HLS 输出目录（运行后生成）
└── build.gradle
```

## 7. 生产化方向
- 使用 **Shaka Packager** 打包 CMAF/HLS/DASH 与 DRM（Widevine/PlayReady/FairPlay）。
- MinIO/S3 作为**输入与输出**统一存储，前端/播放器通过 **CDN** 访问。
- 上传完成由 **事件回调/消息队列**触发作业；多实例转码；失败重试与告警。
- **LL-HLS**（低延迟）与旁路录制 VOD。
