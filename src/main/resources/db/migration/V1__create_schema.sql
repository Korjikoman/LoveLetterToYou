CREATE SEQUENCE my_app_user_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE letter_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE my_app_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100),
    email VARCHAR(320) NOT NULL,
    password VARCHAR(255) NOT NULL,
    reset_token VARCHAR(255)
);

CREATE UNIQUE INDEX uk_my_app_user_email ON my_app_user(email);

CREATE TABLE letter (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    author_email VARCHAR(320) NOT NULL,
    public_token VARCHAR(64) NOT NULL,
    security_key VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(150) NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    images_revision BIGINT NOT NULL DEFAULT 0,
    font_bold BOOLEAN,
    font_cursive BOOLEAN,
    font_underlined BOOLEAN,
    font_family VARCHAR(100),
    font_name VARCHAR(100),
    reaction_code VARCHAR(255)[],
    CONSTRAINT fk_letter_user
        FOREIGN KEY (user_id) REFERENCES my_app_user(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_letter_public_token ON letter(public_token);
CREATE UNIQUE INDEX uk_letter_security_key ON letter(security_key);
CREATE INDEX idx_letter_user_active
    ON letter(user_id, expires_at, id DESC);
CREATE INDEX idx_letter_author_active
    ON letter(lower(author_email), expires_at, id DESC);

CREATE TABLE image (
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

CREATE INDEX idx_image_user_id ON image(user_id);
CREATE INDEX idx_image_expired_upload
    ON image(upload_expires_at) WHERE status = 'UPLOADING';
CREATE INDEX idx_image_expired_ready
    ON image(ready_expires_at) WHERE status = 'READY';

ALTER TABLE my_app_user
    ADD COLUMN avatar_image_id UUID;

CREATE UNIQUE INDEX uk_user_avatar_image
    ON my_app_user(avatar_image_id)
    WHERE avatar_image_id IS NOT NULL;

ALTER TABLE my_app_user
    ADD CONSTRAINT fk_user_avatar_image
    FOREIGN KEY (avatar_image_id) REFERENCES image(id) ON DELETE SET NULL;

CREATE TABLE letter_image (
    id BIGSERIAL PRIMARY KEY,
    letter_id BIGINT NOT NULL,
    image_id UUID NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT fk_letter_image_letter
        FOREIGN KEY (letter_id) REFERENCES letter(id) ON DELETE CASCADE,
    CONSTRAINT fk_letter_image_image
        FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE RESTRICT,
    CONSTRAINT uk_letter_image_image UNIQUE (image_id),
    CONSTRAINT uk_letter_image_position
        UNIQUE (letter_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT check_letter_image_position CHECK (position >= 0)
);

CREATE INDEX idx_letter_image_order
    ON letter_image(letter_id, position);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    claim_token UUID,
    locked_until TIMESTAMPTZ,
    aggregate_type VARCHAR(50),
    aggregate_id VARCHAR(255),
    aggregate_email VARCHAR(255),
    aggregate_version BIGINT,
    image_id UUID,
    temp_rel_path VARCHAR(512),
    main_rel_path VARCHAR(512),
    expected_sha256 VARCHAR(64),
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    dead_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    CONSTRAINT check_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD')),
    CONSTRAINT check_outbox_event_type
        CHECK (event_type IN (
            'LETTER_CACHE_EVICT', 'IMAGE_PROMOTE', 'IMAGE_DELETE'
        )),
    CONSTRAINT check_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_pending
    ON outbox_event(next_attempt_at, created_at, id)
    WHERE status = 'PENDING';
CREATE INDEX idx_outbox_processing
    ON outbox_event(locked_until, created_at, id)
    WHERE status = 'PROCESSING';
CREATE INDEX idx_outbox_image ON outbox_event(image_id);
CREATE INDEX idx_outbox_processed
    ON outbox_event(processed_at) WHERE status = 'PROCESSED';

CREATE TABLE image_quota_lock (
    id SMALLINT PRIMARY KEY
);

INSERT INTO image_quota_lock(id) VALUES (1);
