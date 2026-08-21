CREATE TABLE IF NOT EXISTS outbox_events
(
    id            BIGSERIAL PRIMARY KEY,
    payload       TEXT,
    event_type    VARCHAR(255)                NOT NULL,
    partition_key BIGINT                      NOT NULL,
    status        VARCHAR(50)                 NOT NULL DEFAULT 'NEW',
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    retry_count   INT                         NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status_created_at ON outbox_events (status, created_at);
