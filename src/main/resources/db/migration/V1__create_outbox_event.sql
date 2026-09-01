CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    claim_token UUID NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    
    -- aggregate_type - это письмо (т.е. добавляем письмо в outbox)
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,


    event_type VARCHAR(50) NOT NULL,

    status VARCHAR(50) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,

    locked_until TIMESTAMPTZ,

    processed_at TIMESTAMPTZ,
    dead_at TIMESTAMPTZ,

    last_error VARCHAR(2000),

    CONSTRAINT check_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD')),

    
    CONSTRAINT check_outbox_attempts CHECK (attempts >= 0)

);

CREATE INDEX idx_outbox_pending ON outbox_event(next_attempt_at, created_at, id) WHERE status = 'PENDING';

CREATE INDEX idx_outbox_processing ON outbox_event(lockend_until, created_at, id) WHERE status = 'PROCESSING';