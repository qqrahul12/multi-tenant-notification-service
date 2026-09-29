-- V7: Delivery Attempts (audit trail for each send attempt)
CREATE TABLE delivery_attempts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_request_id UUID        NOT NULL REFERENCES notification_requests(id) ON DELETE CASCADE,
    attempt_number          INT         NOT NULL,
    status                  VARCHAR(20) NOT NULL
                                CHECK (status IN ('SUCCESS', 'FAILURE', 'IN_PROGRESS')),
    error_code              VARCHAR(100),
    error_message           TEXT,
    provider_response       TEXT,
    next_retry_at           TIMESTAMPTZ,
    attempted_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (notification_request_id, attempt_number)
);

CREATE INDEX idx_da_notification_request_id ON delivery_attempts(notification_request_id);
CREATE INDEX idx_da_next_retry_at           ON delivery_attempts(next_retry_at)
    WHERE next_retry_at IS NOT NULL AND status = 'FAILURE';
