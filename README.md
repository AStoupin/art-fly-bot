# ART FLY — Пчелка

Telegram-бот студии ART FLY: менеджер Пчелка записывает клиентов на стрижку.
Диалог ведёт Claude (Spring AI), записи хранятся в CSV.

Стек: Java 21, Spring Boot 4.1, Spring AI 2.0 (Anthropic), TelegramBots 10.

## Как работает

1. Клиент пишет боту (или `/start` — начать заново).
2. Пчелка представляется и по очереди спрашивает имя, дату и время, мастера.
3. Перед записью Пчелка повторяет данные и просит подтвердить, затем вызывает инструмент `createBooking`.
4. `BookingService` проверяет, что мастер есть в справочнике, время в будущем и у мастера на это время
   нет другого клиента, и дописывает строку в `data/records.csv`.

## Данные

| Файл | Структура |
|---|---|
| `data/masters.csv` | имя мастера — по одному на строку |
| `data/records.csv` | имя клиента, дата и время (`yyyy-MM-dd HH:mm`), мастер |

Каталог задаётся свойством `artfly.data-dir` (по умолчанию `data` относительно рабочего каталога).

## Запуск

Нужны ключ Claude API и токен бота от [@BotFather](https://t.me/BotFather):

```bash
export ANTHROPIC_API_KEY=...
export TELEGRAM_BOT_TOKEN=...
./mvnw spring-boot:run
```

Без `TELEGRAM_BOT_TOKEN` приложение стартует, но бот не подключается к Telegram.

Модель и часовой пояс — в `src/main/resources/application.properties`,
текст роли Пчелки — в `src/main/resources/prompts/pchelka-system.st`.

## Тесты

```bash
./mvnw test
```
