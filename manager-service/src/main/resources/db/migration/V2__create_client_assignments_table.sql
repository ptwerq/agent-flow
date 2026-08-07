CREATE TABLE IF NOT EXISTS client_assignments
(
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT      NOT NULL,
    manager_id  BIGINT      NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    assigned_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    CONSTRAINT fk_client_assignments_manager
        FOREIGN KEY (manager_id) REFERENCES managers (id)
);
