-- Инициализация базы app (выполняется образом postgres:16 при первом старте).
-- Схемы создаются и сервисами (schema.sql), поэтому скрипт идемпотентен.

CREATE SCHEMA IF NOT EXISTS users_service;
CREATE SCHEMA IF NOT EXISTS equipment_service;
CREATE SCHEMA IF NOT EXISTS rentals_service;
CREATE SCHEMA IF NOT EXISTS notifications_service;

COMMENT ON SCHEMA users_service IS 'Схема сервиса пользователей';
COMMENT ON SCHEMA equipment_service IS 'Схема сервиса каталога инвентаря';
COMMENT ON SCHEMA rentals_service IS 'Схема сервиса аренды';
COMMENT ON SCHEMA notifications_service IS 'Схема сервиса уведомлений';
