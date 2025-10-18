CREATE TABLE images (
                        id            BIGINT PRIMARY KEY,
                        owner_id      BIGINT      NOT NULL,
                        original_name VARCHAR(255) NOT NULL,
                        content_type  VARCHAR(100) NOT NULL,
                        storage_key   VARCHAR(512) NOT NULL,
                        size_bytes    BIGINT       NOT NULL,
                        width         INT          NULL,
                        height        INT          NULL,
                        sha256_hex    CHAR(64)     NULL,
                        status        VARCHAR(20)  NOT NULL,
                        created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                        updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE image_variants (
                                id            BIGINT PRIMARY KEY,
                                image_id      BIGINT NOT NULL,
                                variant       VARCHAR(50) NOT NULL,
                                width         INT        NULL,
                                height        INT        NULL,
                                format        VARCHAR(10) NOT NULL,
                                storage_key   VARCHAR(512) NOT NULL,
                                size_bytes    BIGINT      NULL,
                                created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE(image_id, variant)
);

CREATE INDEX idx_images_owner ON images(owner_id);
CREATE INDEX idx_images_status ON images(status);