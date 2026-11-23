# Демонстрация минимального сценария (PowerShell 5.1 / PowerShell 7)
#
# Требуется запущенный проект:  docker compose up -d --build
# Запуск:                       powershell -ExecutionPolicy Bypass -File scripts/demo.ps1
#
# Сценарий: создать пользователя -> создать аренду -> получить аренду
#           -> проверить уведомление -> вернуть инвентарь.

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# Корректный вывод кириллицы в консоль Windows PowerShell 5.1
$OutputEncoding = [System.Text.Encoding]::UTF8
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

function Section([string]$title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

function ToJson($value) {
    $value | ConvertTo-Json -Depth 6
}

# Windows PowerShell 5.1 декодирует ответы как ISO-8859-1, поэтому тело
# читаем байтами и явно превращаем UTF-8 в строку.
function Invoke-Api([string]$method, [string]$uri, $payload = $null) {
    $request = @{
        Method           = $method
        Uri              = $uri
        UseBasicParsing  = $true
        TimeoutSec       = 30
    }
    if ($null -ne $payload) {
        $json = $payload | ConvertTo-Json -Depth 6
        $request.ContentType = "application/json; charset=utf-8"
        $request.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
    }

    $response = Invoke-WebRequest @request
    $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }
    $parsed = $text | ConvertFrom-Json
    # PowerShell 5.1 возвращает JSON-массив как один объект — разворачиваем,
    # чтобы @(GetApi ...) и Select-Object работали с элементами списка.
    if ($parsed -is [System.Array]) {
        foreach ($element in $parsed) {
            Write-Output $element
        }
    } else {
        Write-Output $parsed
    }
}

function GetApi([string]$uri) {
    Invoke-Api "GET" $uri
}

function PostJson([string]$uri, $payload) {
    Invoke-Api "POST" $uri $payload
}

Section "1. Проверка API Gateway"
ToJson (GetApi "$BaseUrl/health")

Section "2. Создание пользователя"
$email = "demo+$(Get-Date -Format 'HHmmss')@example.com"
$user = PostJson "$BaseUrl/api/users" @{ name = "Иван Петров"; email = $email }
ToJson $user

Section "3. Каталог инвентаря (первые 3 позиции)"
$equipment = @(GetApi "$BaseUrl/api/equipment")
$equipment | Select-Object -First 3 | ForEach-Object { ToJson $_ }
$item = $equipment | Select-Object -First 1

Section "4. Создание аренды"
$rental = PostJson "$BaseUrl/api/rentals" @{
    userId      = $user.id
    equipmentId = $item.id
    quantity    = 1
    days        = 3
}
ToJson $rental

Section "5. Получение аренды по id"
ToJson (GetApi "$BaseUrl/api/rentals/$($rental.id)")

Section "6. Уведомления (последние 3)"
$notifications = @(GetApi "$BaseUrl/api/notifications?limit=3")
$notifications | ForEach-Object { ToJson $_ }

Section "7. Возврат инвентаря"
ToJson (PostJson "$BaseUrl/api/rentals/$($rental.id)/return" $null)

Section "8. Остаток инвентаря после возврата"
ToJson (GetApi "$BaseUrl/api/equipment/$($item.id)")

Section "Готово"
Write-Host "Сценарий выполнен. Данные сохранены в PostgreSQL (volume db_data)." -ForegroundColor Green
Write-Host "Проверка сохранности: docker compose restart db; затем GET $BaseUrl/api/rentals/$($rental.id)"
