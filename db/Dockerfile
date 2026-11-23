FROM postgres:16

# Скрипты инициализации выполняются только при первом создании volume.
# Схемы также идемпотентно создаются сервисами через schema.sql.
COPY init/01-init.sql /docker-entrypoint-initdb.d/01-init.sql
