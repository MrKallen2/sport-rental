#!/usr/bin/env bash
# Демонстрация минимального сценария (bash / Git Bash / Linux / macOS)
#
# Требуется запущенный проект:  docker compose up -d --build
# Запуск:                       bash scripts/demo.sh
#
# Сценарий: создать пользователя -> создать аренду -> получить аренду
#           -> проверить уведомление -> вернуть инвентарь.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

section() {
  printf '\n=== %s ===\n' "$1"
}

# Достаёт первое числовое поле "id" из JSON без внешних зависимостей.
json_id() {
  grep -o '"id":[[:space:]]*[0-9][0-9]*' | head -n 1 | cut -d: -f2
}

section "1. Проверка API Gateway"
curl -s "$BASE_URL/health"; echo

section "2. Создание пользователя"
EMAIL="demo+$(date +%H%M%S)@example.com"
# Тело отправляем через stdin: в Git Bash для Windows аргументы командной
# строки перекодируются в ANSI, и кириллица в -d ломается.
USER_JSON=$(printf '{"name":"Иван Петров","email":"%s"}' "$EMAIL" \
  | curl -s -X POST "$BASE_URL/api/users" \
      -H "Content-Type: application/json; charset=utf-8" \
      --data-binary @-)
echo "$USER_JSON"
USER_ID=$(printf '%s' "$USER_JSON" | json_id)

section "3. Каталог инвентаря"
EQUIPMENT_JSON=$(curl -s "$BASE_URL/api/equipment")
printf '%s\n' "$EQUIPMENT_JSON" | head -c 700; echo " ..."
EQUIPMENT_ID=$(printf '%s' "$EQUIPMENT_JSON" | json_id)

section "4. Создание аренды"
RENTAL_JSON=$(printf '{"userId":%s,"equipmentId":%s,"quantity":1,"days":3}' "$USER_ID" "$EQUIPMENT_ID" \
  | curl -s -X POST "$BASE_URL/api/rentals" \
      -H "Content-Type: application/json; charset=utf-8" \
      --data-binary @-)
echo "$RENTAL_JSON"
RENTAL_ID=$(printf '%s' "$RENTAL_JSON" | json_id)

section "5. Получение аренды по id"
curl -s "$BASE_URL/api/rentals/$RENTAL_ID"; echo

section "6. Уведомления (последние 3)"
curl -s "$BASE_URL/api/notifications?limit=3"; echo

section "7. Возврат инвентаря"
curl -s -X POST "$BASE_URL/api/rentals/$RENTAL_ID/return"; echo

section "8. Остаток инвентаря после возврата"
curl -s "$BASE_URL/api/equipment/$EQUIPMENT_ID"; echo

section "Готово"
echo "Сценарий выполнен. Данные сохранены в PostgreSQL (volume db_data)."
echo "Проверка сохранности: docker compose restart db; затем GET $BASE_URL/api/rentals/$RENTAL_ID"
