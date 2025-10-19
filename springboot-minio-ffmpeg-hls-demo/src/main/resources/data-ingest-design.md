# 视频入库业务建模与数据流设计

本文档梳理视频入库功能的领域模型、数据流与接口设计，ORM 采用 MyBatis-Plus，便于后续落地实现。

## 1. 业务建模

### 1.1 数据实体与表结构

| 实体                       | 表名                        | 说明                             |
|--------------------------|---------------------------|--------------------------------|
| 视频资源 `VideoAsset`        | `video_asset`             | 视频的主数据（基础元信息、存储位置、状态）          |
| 上传任务 `UploadTask`        | `video_upload_task`       | 记录上传渠道、进度与校验信息                 |
| 转码任务 `TranscodeJob`      | `video_transcode_job`     | FFmpeg 转码任务，关联预设、状态与输出元数据      |
| 转码输出 `TranscodeVariant`  | `video_transcode_variant` | 每个 HLS 分辨率/码率的输出信息             |
| 存储对象 `StorageObject`（可选） | `storage_object`          | 统一抽象 MinIO/Bucket 对象，便于扩展到其他存储 |

#### `video_asset`

| 字段                  | 类型            | 约束       | 说明                  |
|---------------------|---------------|----------|---------------------|
| `id`                | BIGINT        | PK，自增    | 主键                  |
| `title`             | VARCHAR(128)  | NOT NULL | 视频标题                |
| `description`       | VARCHAR(1024) |          | 描述                  |
| `file_name`         | VARCHAR(255)  | NOT NULL | 原始文件名               |
| `file_size`         | BIGINT        | NOT NULL | 文件字节数               |
| `duration_seconds`  | INT           |          | 视频时长                |
| `checksum`          | VARCHAR(64)   | UNIQUE   | 文件哈希，防止重复入库         |
| `source_bucket`     | VARCHAR(64)   | NOT NULL | MinIO bucket        |
| `source_object`     | VARCHAR(512)  | NOT NULL | MinIO 对象键           |
| `cover_image`       | VARCHAR(512)  |          | 封面图路径               |
| `status`            | VARCHAR(32)   | NOT NULL | 状态机（见 1.2）          |
| `transcode_profile` | VARCHAR(64)   |          | 选定转码模板              |
| `ready_at`          | DATETIME      |          | 整体转码完成时间            |
| `created_at`        | DATETIME      | NOT NULL | 创建时间                |
| `updated_at`        | DATETIME      | NOT NULL | 更新时间                |
| `deleted`           | TINYINT       | 默认 0     | MyBatis-Plus 逻辑删除字段 |

#### `video_upload_task`

| 字段                  | 类型           | 约束                     | 说明                    |
|---------------------|--------------|------------------------|-----------------------|
| `id`                | BIGINT       | PK，自增                  | 主键                    |
| `video_id`          | BIGINT       | FK -> `video_asset.id` | 对应视频                  |
| `upload_channel`    | VARCHAR(32)  | NOT NULL               | `WEB`, `API`, `SDK` 等 |
| `uploader_id`       | VARCHAR(64)  |                        | 上传者标识                 |
| `upload_path`       | VARCHAR(512) | NOT NULL               | 临时对象路径                |
| `ingest_strategy`   | VARCHAR(32)  | NOT NULL               | 入库策略（直传、分片、回调）        |
| `progress`          | DECIMAL(5,2) |                        | 百分比                   |
| `checksum`          | VARCHAR(64)  |                        | 上传阶段计算的哈希             |
| `validation_status` | VARCHAR(32)  |                        | 校验结果                  |
| `remark`            | VARCHAR(255) |                        | 失败原因等                 |
| `created_at`        | DATETIME     | NOT NULL               | 创建时间                  |
| `updated_at`        | DATETIME     | NOT NULL               | 更新时间                  |

#### `video_transcode_job`

| 字段               | 类型           | 约束                     | 说明                        |
|------------------|--------------|------------------------|---------------------------|
| `id`             | BIGINT       | PK，自增                  | 主键                        |
| `video_id`       | BIGINT       | FK -> `video_asset.id` | 对应视频                      |
| `job_type`       | VARCHAR(16)  | NOT NULL               | `INGEST`, `RETRANSCODE` 等 |
| `target_profile` | VARCHAR(64)  | NOT NULL               | 目标模板（对应预设表或配置）            |
| `priority`       | TINYINT      | NOT NULL               | 调度优先级                     |
| `status`         | VARCHAR(32)  | NOT NULL               | 任务状态                      |
| `error_code`     | VARCHAR(32)  |                        | 错误码                       |
| `error_message`  | VARCHAR(512) |                        | 错误详情                      |
| `started_at`     | DATETIME     |                        | 开始时间                      |
| `finished_at`    | DATETIME     |                        | 完成时间                      |
| `created_at`     | DATETIME     | NOT NULL               | 入库时间                      |
| `updated_at`     | DATETIME     | NOT NULL               | 更新时间                      |

#### `video_transcode_variant`

| 字段                    | 类型           | 约束                             | 说明                |
|-----------------------|--------------|--------------------------------|-------------------|
| `id`                  | BIGINT       | PK，自增                          | 主键                |
| `job_id`              | BIGINT       | FK -> `video_transcode_job.id` | 所属任务              |
| `video_id`            | BIGINT       | FK -> `video_asset.id`         | 对应视频              |
| `variant_level`       | INT          | NOT NULL                       | 变体序号（v0/v1/v2）    |
| `resolution`          | VARCHAR(16)  | NOT NULL                       | 例如 `1920x1080`    |
| `bitrate_kbps`        | INT          |                                | 码率                |
| `playlist_path`       | VARCHAR(512) | NOT NULL                       | HLS playlist 相对路径 |
| `segment_path_prefix` | VARCHAR(512) | NOT NULL                       | TS/fragment 前缀    |
| `duration_seconds`    | INT          |                                | 变体时长              |
| `checksum`            | VARCHAR(64)  |                                | 校验码               |
| `created_at`          | DATETIME     | NOT NULL                       | 创建时间              |
| `updated_at`          | DATETIME     | NOT NULL                       | 更新时间              |

> 如果后续需要跨存储（本地、S3、OSS），可引入 `storage_object` 表，抽象对象的 bucket、region、加密策略等。

### 1.2 状态模型

`VideoAsset.status` 建议定义状态枚举：

| 状态           | 说明         | 触发条件                 |
|--------------|------------|----------------------|
| `UPLOADING`  | 客户端上传中     | 上传开始                 |
| `UPLOADED`   | 上传完成待校验    | 上传完成事件               |
| `VALIDATED`  | 校验通过、等待转码  | 校验成功                 |
| `PROCESSING` | 转码中        | 转码任务开始               |
| `READY`      | 转码完成，可对外播放 | 所有 `TranscodeJob` 成功 |
| `FAILED`     | 入库失败       | 上传/校验/转码任何阶段失败       |
| `ARCHIVED`   | 下线或归档      | 管理操作                 |

`TranscodeJob.status` 建议包含 `PENDING`, `DISPATCHED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`。

### 1.3 MyBatis-Plus 建模建议

- 实体类放在 `com.example.videodemo.persistence.entity`，使用 `@TableName` 与 `@TableId`。
- 统一继承 `BaseEntity`（内含 `createdAt`, `updatedAt`, `deleted`），配合 `MetaObjectHandler` 自动填充。
- Mapper 接口放在 `com.example.videodemo.persistence.mapper`，继承 `BaseMapper<T>`。
- 自定义查询/分页可通过 `QueryWrapper`、`LambdaQueryWrapper`。
- 利用逻辑删除（`deleted` 字段）和乐观锁（如需要，可加 `version` 字段与 `@Version`）。

## 2. 数据流设计

### 2.1 同步入库流程（上传完成到入库）

```
Uploader
  -> IngestController.POST /api/videos/upload
    -> VideoIngestService.prepareUpload()
      -> VideoAssetMapper.insert()  // 创建 UPLOADING 记录
      -> UploadTaskMapper.insert()
```

上传完成回调：

```
Uploader / 客户端
  -> IngestController.POST /api/videos/{id}/complete
    -> VideoIngestService.completeUpload()
      -> ValidationComponent.validateObject()
      -> Transactional {
           VideoAssetMapper.updateStatus(VALIDATED/FAILED)
           UploadTaskMapper.updateProgress(100)
           TranscodeJobMapper.insert()
         }
      -> EventPublisher.publish(IngestCompletedEvent)
```

### 2.2 异步转码与回写

```
EventListener (IngestCompletedEvent)
  -> TranscodeDispatchService.dispatch()
    -> TranscodeJobMapper.updateStatus(DISPATCHED)
    -> FfmpegWorker.consume(job)
      -> MinIO 下载源文件
      -> 执行 FFmpeg
      -> MinIO 上传 HLS 产物
      -> VariantMetadataCollector.persist()
           -> TranscodeVariantMapper.batchInsert()
      -> Transactional {
           TranscodeJobMapper.updateStatus(SUCCESS)
           if all jobs success -> VideoAssetMapper.updateStatus(READY, readyAt)
         }
      -> On failure -> update job status FAILED, VideoAsset 状态置 FAILED
```

补偿机制：

- 转码任务失败时写入 `errorCode` / `errorMessage`，后台可支持重试（新建 `RETRANSCODE` 任务）。
- 通过定时任务扫描长时间 `RUNNING` 的任务，触发告警或人工介入。

### 2.3 元数据同步与缓存

- 视频 READY 后可通过事件刷新前端缓存或下发消息（如通知服务）。
- 若未来接入搜索，可在 READY 时将核心元数据写入搜索引擎（ElasticSearch 等）。

## 3. 接口设计

### 3.1 REST API

| Method | Path                                     | 描述             | 请求体                                                | 响应                                                |
|--------|------------------------------------------|----------------|----------------------------------------------------|---------------------------------------------------|
| `POST` | `/api/videos/upload`                     | 创建上传任务，返回上传凭证  | `CreateUploadRequest`（标题、描述、文件信息）                  | `UploadTicketResponse`（videoId、uploadTaskId、临时凭证） |
| `PUT`  | `/api/videos/{videoId}/chunks/{chunkId}` | 分片上传（可选）       | 二进制或表单                                             | 状态码                                               |
| `POST` | `/api/videos/{videoId}/complete`         | 上传完成回调，触发校验与入库 | `CompleteUploadRequest`（checksum、duration、profile） | `VideoDetailResponse`                             |
| `GET`  | `/api/videos/{videoId}`                  | 查询视频元数据        | 无                                                  | `VideoDetailResponse`                             |
| `GET`  | `/api/videos`                            | 分页查询视频列表       | 查询参数（状态、关键词、分页）                                    | `Page<VideoSummary>`                              |
| `POST` | `/api/videos/{videoId}/transcode/retry`  | 重试转码           | `RetryTranscodeRequest`                            | `TranscodeJobResponse`                            |

说明：

- 上传接口返回 MinIO 上传凭证（STS、预签名 URL）与目标 bucket/object。
- 完成接口需幂等，重复调用不应导致多次转码。

### 3.2 服务层接口

```java
public interface VideoIngestService {
    UploadTicket createUpload(CreateUploadCommand command);
    VideoAssetDetail completeUpload(CompleteUploadCommand command);
    void markUploadFailed(Long videoId, String reason);
}

public interface TranscodeDispatchService {
    void dispatchIngestJob(Long videoId, String profile);
    void handleJobSuccess(Long jobId, TranscodeResult result);
    void handleJobFailure(Long jobId, String errorCode, String errorMessage);
}
```

- Command/DTO 位于 `service.dto`，隔离控制层与实体。
- 入库主事务在 `completeUpload` 内部，MyBatis-Plus Mapper 通过构造的实体批量更新。
- 转码结果回写由工作线程或监听器调用 `handleJobSuccess/Failure`。

### 3.3 MyBatis-Plus Mapper

```java
public interface VideoAssetMapper extends BaseMapper<VideoAsset> {
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("readyAt") LocalDateTime readyAt);
}

public interface VideoTranscodeJobMapper extends BaseMapper<TranscodeJob> {
    List<TranscodeJob> selectPendingJobs(@Param("limit") int limit);
}
```

- 自定义 XML 仅在复杂查询需要时创建（放在 `src/main/resources/mapper`）。
- 通过 `@TableLogic` 管理逻辑删除，`@Version` 管理并发更新。

### 3.4 事件接口

- `IngestCompletedEvent`：携带 `videoId`、`profile`、校验结果。
- `TranscodeFinishedEvent`：由转码工作流发布，通知前端或其他系统。
- 事件分发可先使用 Spring `ApplicationEventPublisher`，后续可平滑迁移到 MQ。

---

该设计文档为入库功能的蓝图，可基于此逐步实现实体、Mapper、Service 与 Controller，并逐层补充测试与异常处理。
