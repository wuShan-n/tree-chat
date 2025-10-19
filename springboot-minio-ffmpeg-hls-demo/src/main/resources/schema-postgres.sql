CREATE TABLE IF NOT EXISTS video_asset
(
    id BIGSERIAL PRIMARY KEY,
    title             VARCHAR(128) NOT NULL,
    description       VARCHAR(1024),
    file_name         VARCHAR(255) NOT NULL,
    file_size         BIGINT       NOT NULL,
    duration_seconds  INT,
    checksum          VARCHAR(64) UNIQUE,
    source_bucket     VARCHAR(64)  NOT NULL,
    source_object     VARCHAR(512) NOT NULL,
    cover_image       VARCHAR(512),
    status            VARCHAR(32)  NOT NULL,
    transcode_profile VARCHAR(64),
    ready_at          TIMESTAMP,
    playback_url      VARCHAR(512),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS video_upload_task
(
    id BIGSERIAL PRIMARY KEY,
    video_id          BIGINT NOT NULL REFERENCES video_asset (id),
    upload_channel    VARCHAR(32),
    uploader_id       VARCHAR(64),
    upload_path       VARCHAR(512),
    ingest_strategy   VARCHAR(32),
    progress          NUMERIC(5, 2),
    checksum          VARCHAR(64),
    validation_status VARCHAR(32),
    remark            VARCHAR(255),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS video_transcode_job
(
    id BIGSERIAL PRIMARY KEY,
    video_id       BIGINT NOT NULL REFERENCES video_asset (id),
    job_type       VARCHAR(16),
    target_profile VARCHAR(64),
    priority       INT,
    status         VARCHAR(32),
    error_code     VARCHAR(32),
    error_message  VARCHAR(512),
    started_at     TIMESTAMP,
    finished_at    TIMESTAMP,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS video_transcode_variant
(
    id BIGSERIAL PRIMARY KEY,
    job_id              BIGINT NOT NULL REFERENCES video_transcode_job (id),
    video_id            BIGINT NOT NULL REFERENCES video_asset (id),
    variant_level       INT,
    resolution          VARCHAR(16),
    bitrate_kbps        INT,
    playlist_path       VARCHAR(512),
    segment_path_prefix VARCHAR(512),
    duration_seconds    INT,
    checksum            VARCHAR(64),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT  DEFAULT 0
);
