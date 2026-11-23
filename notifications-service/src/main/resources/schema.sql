CREATE SCHEMA IF NOT EXISTS notifications_service;

CREATE TABLE IF NOT EXISTS notifications_service.notifications (
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(60)  NOT NULL,
    recipient  VARCHAR(180) NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS notifications_created_idx
    ON notifications_service.notifications (created_at DESC);
