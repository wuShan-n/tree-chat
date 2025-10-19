-- DDL for video transcoding service (PostgreSQL)

-- ----------------------------
-- 核心表：视频资产
-- 存储有关视频文件的所有核心元数据。
-- 这是系统中所有其他表的中心“名词”。
-- ----------------------------
CREATE TABLE IF NOT EXISTS video_asset
(
    id BIGSERIAL PRIMARY KEY,
    title             VARCHAR(128) NOT NULL,            -- 视频标题
    description       VARCHAR(1024),                    -- 视频描述
    file_name         VARCHAR(255) NOT NULL,            -- 原始上传的文件名 (e.g., "my_vacation.mp4")
    file_size         BIGINT       NOT NULL,            -- 原始文件大小（字节）
    duration_seconds  INT,                              -- 视频时长（秒），通常在转码前或转码期间填充
    checksum          VARCHAR(64) UNIQUE,               -- 原始文件的校验和 (e.g., MD5, SHA256)，用于完整性检查
    source_bucket     VARCHAR(64)  NOT NULL,            -- 原始视频文件所在的 S3 存储桶
    source_object     VARCHAR(512) NOT NULL,            -- 原始视频文件所在的 S3 对象键 (路径)
    cover_image       VARCHAR(512),                     -- 封面图 URL 或 S3 路径
    status            VARCHAR(32)  NOT NULL,            -- 资产的总体状态 (e.g., UPLOADING, PROCESSING, READY, FAILED)
    transcode_profile VARCHAR(64),                      -- 应用于此资产的转码配置文件名称 (e.g., "default-hls", "audio-only")
    ready_at          TIMESTAMP,                        -- 视频转码完成并可播放的时间
    playback_url      VARCHAR(512),                     -- 主播放列表的 URL (e.g., "master.m3u8" 的 CDN 路径)
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 记录创建时间
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 记录最后更新时间
    deleted           SMALLINT  DEFAULT 0                 -- 逻辑删除标志 (0=未删除, 1=已删除)
);

-- ----------------------------
-- 跟踪表：视频上传任务
-- 记录每一次上传尝试。一个 video_asset 可以有多次上传尝试（例如，失败后重传）。
-- ----------------------------
CREATE TABLE IF NOT EXISTS video_upload_task
(
    id BIGSERIAL PRIMARY KEY,
    video_id          BIGINT NOT NULL REFERENCES video_asset (id), -- 关联的视频资产
    upload_channel    VARCHAR(32),                      -- 上传渠道 (e.g., "WEB", "API", "APP")
    uploader_id       VARCHAR(64),                      -- 上传者的用户ID
    upload_path       VARCHAR(512),                     -- 上传到 S3 的对象键 (可能与 video_asset.source_object 相同)
    ingest_strategy   VARCHAR(32),                      -- 注入策略 (e.g., "PRESIGN" 预签名上传, "DIRECT" S3直接注入)
    progress          NUMERIC(5, 2),                    -- 上传进度 (0.00 - 100.00)
    checksum          VARCHAR(64),                      -- 上传期间客户端计算的校验和
    validation_status VARCHAR(32),                      -- 此上传任务的验证状态 (e.g., "PENDING", "PASSED", "FAILED")
    remark            VARCHAR(255),                     -- 备注，通常用于记录验证失败的原因
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT  DEFAULT 0
);

-- ----------------------------
-- 跟踪表：视频转码作业
-- 记录每一次转码作业。一个 video_asset 可以有多个转码作业（例如，初次注入、后续重转码）。
-- ----------------------------
CREATE TABLE IF NOT EXISTS video_transcode_job
(
    id BIGSERIAL PRIMARY KEY,
    video_id       BIGINT NOT NULL REFERENCES video_asset (id), -- 关联的视频资产
    job_type       VARCHAR(16),                      -- 作业类型 (e.g., "INGEST", "RETRANSCODE")
    target_profile VARCHAR(64),                      -- 此作业使用的目标转码配置文件
    priority       INT,                              -- 作业优先级（用于队列）
    status         VARCHAR(32),                      -- 作业状态 (e.g., "PENDING", "RUNNING", "SUCCESS", "FAILED")
    error_code     VARCHAR(32),                      -- 失败时的错误代码
    error_message  VARCHAR(512),                     -- 失败时的详细错误信息
    started_at     TIMESTAMP,                        -- 作业开始执行的时间
    finished_at    TIMESTAMP,                        -- 作业完成（成功或失败）的时间
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT  DEFAULT 0
);

-- ----------------------------
-- 结果表：视频转码变体
-- 存储转码作业的产物。一个 job 会产生多个 variant (e.g., 1080p, 720p, 480p)。
-- ----------------------------
CREATE TABLE IF NOT EXISTS video_transcode_variant
(
    id BIGSERIAL PRIMARY KEY,
    job_id              BIGINT NOT NULL REFERENCES video_transcode_job (id), -- 关联的转码作业
    video_id            BIGINT NOT NULL REFERENCES video_asset (id),       -- 关联的视频资产 (冗余字段，便于查询)
    variant_level       INT,                              -- 变体级别（用于排序或标识）
    resolution          VARCHAR(16),                      -- 分辨率 (e.g., "1920x1080", "720p")
    bitrate_kbps        INT,                              -- 视频比特率 (kbps)
    playlist_path       VARCHAR(512),                     -- 此变体的 HLS 播放列表的 S3 路径 (e.g., "vod/path/v0/stream.m3u8")
    segment_path_prefix VARCHAR(512),                     -- 此变体的 HLS 片段的 S3 路径前缀 (e.g., "vod/path/v0/seg_")
    duration_seconds    INT,                              -- 此变体的确切时长
    checksum            VARCHAR(64),                      -- 播放列表文件或所有片段的组合校验和
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT  DEFAULT 0
);