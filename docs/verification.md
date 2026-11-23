# Проверка работы системы (фактический вывод)

Дата проверки: **23.11.2026**
Окружение: Windows 11, Docker Desktop 29.8.1, Docker Compose v5.5.1.
Все команды выполнялись из корня репозитория.

---

## 1. Запуск с нуля

```bash
docker compose down -v      # удаляем и контейнеры, и volume — проверяем старт «с чистого листа»
docker compose up -d
```

Ключевые строки запуска:

```
Container sport-rental-db Created
Container sport-rental-users Created
Container sport-rental-equipment Created
Container sport-rental-notifications Created
Container sport-rental-rentals Created
Container sport-rental-gateway Created
Container sport-rental-db Healthy
Container sport-rental-users Started
Container sport-rental-equipment Started
Container sport-rental-notifications Started
Container sport-rental-rentals Started
Container sport-rental-gateway Started
```

```bash
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
```

```
NAME                         STATUS                    PORTS
sport-rental-db              Up 27 seconds (healthy)   0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp
sport-rental-equipment       Up 21 seconds             0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
sport-rental-gateway         Up 20 seconds             0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
sport-rental-notifications   Up 21 seconds             0.0.0.0:8084->8084/tcp, [::]:8084->8084/tcp
sport-rental-rentals         Up 20 seconds             0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp
sport-rental-users           Up 22 seconds             0.0.0.0:8081->8081/tcp, [::]:8081->8081/tcp
```

Шесть контейнеров: PostgreSQL, четыре микросервиса и API Gateway.

---

## 2. Health-check через API Gateway

```bash
curl http://localhost:8080/health
```

```json
{"status":"UP","service":"api-gateway","time":"2026-11-23T09:06:17.310391100Z"}
```

```bash
curl http://localhost:8080/
```

```json
{
  "service": "sport-rental api-gateway",
  "description": "Прокат спортивного инвентаря: единая точка входа",
  "routes": {
    "/api/equipment/**": "http://equipment-service:8082",
    "/api/notifications/**": "http://notifications-service:8084",
    "/api/rentals/**": "http://rentals-service:8083",
    "/api/users/**": "http://users-service:8081"
  }
}
```

---

## 3. Минимальный сценарий (`scripts/demo.ps1`)

Запуск:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo.ps1
```

Тот же сценарий проверен и в bash-версии (`bash scripts/demo.sh` из Git Bash): скрипт создал пользователя `demo+161011@example.com`, аренду `#3` и вернул инвентарь — ответы совпали с приведёнными ниже.

### 3.1. Создание пользователя

```json
{
  "id": 1,
  "name": "Иван Петров",
  "email": "demo+160617@example.com",
  "createdAt": "2026-11-23T09:06:17.840992Z"
}
```

### 3.2. Каталог инвентаря (первые 3 позиции)

```json
[
  {"id":1,"name":"Горные лыжи","category":"Лыжи","dailyPrice":1200.00,"totalQuantity":6,"availableQuantity":6},
  {"id":2,"name":"Сноуборд","category":"Сноуборд","dailyPrice":1500.00,"totalQuantity":4,"availableQuantity":4},
  {"id":3,"name":"Велосипед горный","category":"Велосипеды","dailyPrice":900.00,"totalQuantity":8,"availableQuantity":8}
]
```

### 3.3. Создание аренды

```json
{
  "id": 1,
  "userId": 1,
  "userName": "Иван Петров",
  "equipmentId": 1,
  "equipmentName": "Горные лыжи",
  "quantity": 1,
  "days": 3,
  "totalPrice": 3600.00,
  "status": "ACTIVE",
  "createdAt": "2026-11-23T09:06:18.469969Z",
  "returnedAt": null
}
```

Стоимость рассчитана сервисом аренды: `1200.00 × 1 × 3 = 3600.00`.

### 3.4. Получение аренды по id

```bash
curl http://localhost:8080/api/rentals/1
```

Ответ содержит ту же аренду со статусом `ACTIVE` — данные реально сохранены, а не возвращены из памяти.

### 3.5. Уведомление

```json
{
  "id": 1,
  "type": "RENTAL_CREATED",
  "recipient": "demo+160617@example.com",
  "message": "Аренда #1 оформлена: Горные лыжи, 1 шт. на 3 дн., сумма 3600.00 руб.",
  "createdAt": "2026-11-23T09:06:18.675091Z"
}
```

Уведомление создал `notifications-service` по HTTP-вызову из `rentals-service`.

### 3.6. Возврат инвентаря

```bash
curl -X POST http://localhost:8080/api/rentals/1/return
```

```json
{
  "id": 1,
  "status": "RETURNED",
  "createdAt": "2026-11-23T09:06:18.469969Z",
  "returnedAt": "2026-11-23T09:06:18.811206Z"
}
```

### 3.7. Остаток восстановился

```bash
curl http://localhost:8080/api/equipment/1
```

```json
{"id":1,"name":"Горные лыжи","category":"Лыжи","dailyPrice":1200.00,"totalQuantity":6,"availableQuantity":6}
```

Было 6 → при аренде стало 5 → после возврата снова 6.

---

## 4. Взаимодействие сервисов (логи)

```bash
docker compose logs rentals-service | grep "HTTP"
```

```
HTTP -> users-service GET /users/1
HTTP <- users-service GET /users/1: Иван Петров <demo+160617@example.com>
HTTP -> equipment-service GET /equipment/2
HTTP <- equipment-service: Сноуборд (available 4/4)
HTTP -> equipment-service POST /equipment/2/reserve quantity=2
HTTP <- equipment-service reserve: available 2/4
Rental #2 created: user=demo+160617@example.com equipment=Сноуборд qty=2 days=2 total=6000.00
HTTP -> notifications-service POST /notifications type=RENTAL_CREATED
HTTP <- notifications-service: notification accepted
```

```bash
docker compose logs notifications-service | grep "NOTIFICATION SENT"
```

```
NOTIFICATION SENT id=1 type=RENTAL_CREATED to=demo+160617@example.com message="Аренда #1 оформлена: Горные лыжи, 1 шт. на 3 дн., сумма 3600.00 руб."
NOTIFICATION SENT id=2 type=RENTAL_RETURNED to=Иван Петров message="Аренда #1 закрыта: Горные лыжи, 1 шт. возвращены."
NOTIFICATION SENT id=3 type=RENTAL_CREATED to=demo+160617@example.com message="Аренда #2 оформлена: Сноуборд, 2 шт. на 2 дн., сумма 6000.00 руб."
```

API Gateway логирует маршрут и сквозной `X-Request-Id`:

```bash
docker compose logs api-gateway | grep PROXY
```

```
PROXY POST /rentals -> rentals 404 (46 ms) requestId=7ffae166-...
PROXY POST /rentals -> rentals 409 (48 ms) requestId=2697f33a-...
PROXY POST /rentals -> rentals 201 (32 ms) requestId=3bedb5e9-...
PROXY GET /rentals/2 -> rentals 200 (27 ms) requestId=300f93fd-...
```

---

## 5. Обработка ошибок

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"not-an-email"}'
```

```
HTTP/1.1 400
{"timestamp":"2026-11-23T09:06:29.164744600Z","status":400,"error":"email: must be a well-formed email address"}
```

```bash
curl -i -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":9999,"equipmentId":1,"quantity":1,"days":3}'
```

```
HTTP/1.1 404
{"timestamp":"2026-11-23T09:06:29.160913600Z","status":404,"error":"User with id=9999 not found in users-service"}
```

Ошибку вернул `users-service`, `rentals-service` её обработал и вернул понятное сообщение.

```bash
curl -i -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":1,"quantity":100,"days":1}'
```

```
HTTP/1.1 409
{"timestamp":"2026-11-23T09:06:29.182330300Z","status":409,"error":"Not enough units of 'Горные лыжи': requested=100, available=6"}
```

```bash
# 400 — некорректный JSON (обрезанное тело / неверный тип поля)
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"broken'
```

```
HTTP/1.1 400
{"timestamp":"2026-11-23T09:20:47.293262200Z","status":400,"error":"Malformed or unreadable JSON request body"}
```

---

## 6. Сохранность данных после перезапуска PostgreSQL

```bash
# создаём активную аренду #2
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":2,"quantity":2,"days":2}'
```

```json
{"id":2,"userId":1,"userName":"Иван Петров","equipmentId":2,"equipmentName":"Сноуборд","quantity":2,"days":2,"totalPrice":6000.00,"status":"ACTIVE","createdAt":"2026-11-23T09:06:34.385146Z","returnedAt":null}
```

```bash
docker compose restart db
sleep 15
docker compose ps db
```

```
NAME              STATUS
sport-rental-db   Up 16 seconds (healthy)
```

```bash
curl http://localhost:8080/api/rentals/2
```

```json
{"id":2,"userId":1,"userName":"Иван Петров","equipmentId":2,"equipmentName":"Сноуборд","quantity":2,"days":2,"totalPrice":6000.00,"status":"ACTIVE","createdAt":"2026-11-23T09:06:34.385146Z","returnedAt":null}
```

```bash
curl http://localhost:8080/api/equipment/2
```

```json
{"id":2,"name":"Сноуборд","category":"Сноуборд","dailyPrice":1500.00,"totalQuantity":4,"availableQuantity":2}
```

Данные и остаток (`availableQuantity=2`) сохранились, хотя контейнер БД был перезапущен.

---

## 7. Проверка данных напрямую в PostgreSQL

```bash
docker compose exec db psql -U postgres -d app -c "\dn" \
  -c "SELECT id, user_id, equipment_id, quantity, days, total_price, status FROM rentals_service.rentals ORDER BY id;"
```

```
              List of schemas
         Name          |       Owner
-----------------------+-------------------
 equipment_service     | postgres
 notifications_service | postgres
 public                | pg_database_owner
 rentals_service       | postgres
 users_service         | postgres
(5 rows)

 id | user_id | equipment_id | quantity | days | total_price |  status
----+---------+--------------+----------+------+-------------+----------
  1 |       1 |            1 |        1 |    3 |     3600.00 | RETURNED
  2 |       1 |            2 |        2 |    2 |     6000.00 | ACTIVE
(2 rows)
```

Одна база `app`, четыре схемы, данные лежат в volume `db_data`.

---

## 8. Итог проверки

| Что проверено | Результат |
| --- | --- |
| `docker compose up -d` с нуля | 6 контейнеров запущены, `db` healthy |
| Все сервисы отвечают через Gateway | да (`/health`, `/api/**`) |
| Минимум 3 сервиса | 4 сервиса + gateway |
| Взаимодействие сервисов по HTTP | видно в логах `rentals-service` |
| PostgreSQL как отдельный сервис `db` | да, `postgres:16` |
| Volume `db_data` | данные пережили `docker compose restart db` |
| Dockerfile у каждого сервиса | 6 Dockerfile (5 сервисов + db) |
| C4 Context / Container / Component | `docs/architecture-*.png`, `docs/architecture.drawio` |
| Ошибки 400 / 404 / 409 | возвращаются с понятным JSON (в т.ч. некорректный JSON → 400) |
| Логи в консоль | `docker compose logs` показывает запросы и бизнес-события |
