CREATE SCHEMA IF NOT EXISTS equipment_service;

CREATE TABLE IF NOT EXISTS equipment_service.equipment (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(120)  NOT NULL UNIQUE,
    category           VARCHAR(80)   NOT NULL,
    daily_price        NUMERIC(10, 2) NOT NULL CHECK (daily_price >= 0),
    total_quantity     INTEGER       NOT NULL CHECK (total_quantity > 0),
    available_quantity INTEGER       NOT NULL CHECK (available_quantity >= 0),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Демо-каталог. Повторный запуск не создаёт дубликаты.
INSERT INTO equipment_service.equipment (name, category, daily_price, total_quantity, available_quantity)
VALUES ('Горные лыжи', 'Лыжи', 1200.00, 6, 6),
       ('Сноуборд', 'Сноуборд', 1500.00, 4, 4),
       ('Велосипед горный', 'Велосипеды', 900.00, 8, 8),
       ('Палатка 4-местная', 'Туризм', 600.00, 5, 5),
       ('SUP-доска', 'Водный спорт', 1000.00, 3, 3)
ON CONFLICT (name) DO NOTHING;
