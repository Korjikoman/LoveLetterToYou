CREATE TABLE IF NOT EXISTS image (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    temp_rel_path VARCHAR(512),
    main_rel_path VARCHAR(512) NOT NULL,
    image_purpose VARCHAR(40) NOT NULL,
    extension VARCHAR(16),
    status VARCHAR(40) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    height INTEGER,
    width INTEGER,

    ready_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    ready_expires_at TIMESTAMPTZ,
    upload_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    sha256 VARCHAR(64),

    CONSTRAINT fk_image_user
        FOREIGN KEY (user_id) REFERENCES my_app_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_image_main_path UNIQUE (main_rel_path),
    CONSTRAINT check_image_purpose
        CHECK (image_purpose IN ('LETTER', 'AVATAR')),
    CONSTRAINT check_image_status
        CHECK (status IN (
            'UPLOADING', 'STAGED', 'READY', 'ATTACHED',
            'DELETE_AFTER_PROMOTION', 'DELETE_PENDING', 'DELETED', 'FAILED'
        )),
    CONSTRAINT check_image_size
        CHECK (size_bytes IS NULL OR size_bytes > 0),
    CONSTRAINT check_image_dimensions CHECK (
        (width IS NULL AND height IS NULL)
        OR (width > 0 AND height > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_image_user_id ON image(user_id);
CREATE INDEX IF NOT EXISTS idx_image_expired_upload
    ON image(upload_expires_at) WHERE status = 'UPLOADING';
CREATE INDEX IF NOT EXISTS idx_image_expired_ready
    ON image(ready_expires_at) WHERE status = 'READY';
