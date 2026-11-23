CREATE SCHEMA IF NOT EXISTS users_service;

CREATE TABLE IF NOT EXISTS users_service.users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(180) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
