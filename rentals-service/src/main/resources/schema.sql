CREATE SCHEMA IF NOT EXISTS rentals_service;

CREATE TABLE IF NOT EXISTS rentals_service.rentals (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    user_name      VARCHAR(120) NOT NULL,
    equipment_id   BIGINT       NOT NULL,
    equipment_name VARCHAR(120) NOT NULL,
    quantity       INTEGER      NOT NULL CHECK (quantity > 0),
    days           INTEGER      NOT NULL CHECK (days > 0),
    total_price    NUMERIC(12, 2) NOT NULL CHECK (total_price >= 0),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    returned_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS rentals_user_idx ON rentals_service.rentals (user_id);
CREATE INDEX IF NOT EXISTS rentals_status_idx ON rentals_service.rentals (status);
