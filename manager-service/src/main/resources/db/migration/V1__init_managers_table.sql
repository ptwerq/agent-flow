CREATE TABLE IF NOT EXISTS managers
(
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    phone        VARCHAR(50)  NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    max_capacity INT          NOT NULL DEFAULT 5,
    current_load INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE
);
