CREATE TABLE IF NOT EXISTS outbox_event (
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
        CHECK (event_type IN ('LETTER_CACHE_EVICT', 'IMAGE_PROMOTE', 'IMAGE_DELETE')),
    CONSTRAINT check_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox_event(next_attempt_at, created_at, id)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_outbox_processing
    ON outbox_event(locked_until, created_at, id)
    WHERE status = 'PROCESSING';
CREATE INDEX IF NOT EXISTS idx_outbox_image ON outbox_event(image_id);
CREATE INDEX IF NOT EXISTS idx_outbox_processed
    ON outbox_event(processed_at) WHERE status = 'PROCESSED';
