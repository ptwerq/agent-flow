--liquibase formatted sql
--changeset agentflow:1-create-clients-table
CREATE TABLE clients
(
    id          BIGSERIAL    PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(50)  NOT NULL,
    manager_id  BIGINT,
    deal_status VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_clients_email UNIQUE (email),
    CONSTRAINT uq_clients_phone UNIQUE (phone)
);
