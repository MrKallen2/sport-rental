# Примеры запросов curl

Все запросы идут через API Gateway: `http://localhost:8080`.
Перед запуском поднимите систему: `docker compose up -d --build`.

> **Windows/PowerShell.** В `cmd` и PowerShell кавычки отличаются: используйте
> `curl.exe` и экранирование `\"` (как в примерах ниже) либо запускайте
> bash-вариант из Git Bash. Готовый сценарий без ручного ввода:
> `powershell -ExecutionPolicy Bypass -File scripts/demo.ps1`.

## Health

```bash
curl http://localhost:8080/health
curl http://localhost:8080/
```

## Users

```bash
# создать пользователя
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван Петров","email":"ivan@example.com"}'

# список пользователей
curl http://localhost:8080/api/users

# пользователь по id
curl http://localhost:8080/api/users/1
```

Windows PowerShell:

```powershell
curl.exe -s -X POST http://localhost:8080/api/users `
  -H "Content-Type: application/json" `
  -d '{\"name\":\"Иван Петров\",\"email\":\"ivan@example.com\"}'
```

## Equipment

```bash
# каталог целиком
curl http://localhost:8080/api/equipment

# фильтр по категории
curl -G http://localhost:8080/api/equipment --data-urlencode "category=Лыжи"

# одна позиция
curl http://localhost:8080/api/equipment/1

# добавить позицию каталога
curl -X POST http://localhost:8080/api/equipment \
  -H "Content-Type: application/json" \
  -d '{"name":"Каяк","category":"Водный","dailyPrice":1100.00,"totalQuantity":3}'

# зарезервировать и освободить единицы (обычно вызывается rentals-service)
curl -X POST http://localhost:8080/api/equipment/1/reserve \
  -H "Content-Type: application/json" -d '{"quantity":1}'
curl -X POST http://localhost:8080/api/equipment/1/release \
  -H "Content-Type: application/json" -d '{"quantity":1}'
```

## Rentals

```bash
# создать аренду: пользователь 1, инвентарь 1, 1 шт. на 3 дня
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":1,"quantity":1,"days":3}'

# список аренд (можно фильтровать)
curl http://localhost:8080/api/rentals
curl "http://localhost:8080/api/rentals?userId=1&status=ACTIVE"

# аренда по id
curl http://localhost:8080/api/rentals/1

# вернуть инвентарь
curl -X POST http://localhost:8080/api/rentals/1/return
```

## Notifications

```bash
curl "http://localhost:8080/api/notifications?limit=10"

curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{"type":"MANUAL","recipient":"ivan@example.com","message":"Тестовое уведомление"}'
```

## Проверка ошибок

```bash
# 400 — невалидный email
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"not-an-email"}'

# 400 — некорректный JSON (обрезанное тело)
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"broken'

# 404 — несуществующий пользователь
curl -i -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":9999,"equipmentId":1,"quantity":1,"days":3}'

# 404 — несуществующая аренда
curl -i http://localhost:8080/api/rentals/9999

# 409 — недостаточно инвентаря на складе
curl -i -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":1,"quantity":100,"days":1}'

# 409 — повторный возврат уже закрытой аренды
curl -i -X POST http://localhost:8080/api/rentals/1/return
```

## Полезные команды Docker

```bash
# статус контейнеров
docker compose ps

# логи (в т.ч. HTTP-вызовы между сервисами и X-Request-Id)
docker compose logs -f rentals-service
docker compose logs -f api-gateway

# прямой запрос в PostgreSQL
docker compose exec db psql -U postgres -d app \
  -c "SELECT id, status, total_price FROM rentals_service.rentals ORDER BY id;"

# проверка сохранности данных
docker compose restart db
```
