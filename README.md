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

## Выкладка на сервер

Бот работает на VPS 204.77.1.43 как служба systemd `art-fly-bot` из `/opt/art-fly-bot`.

**Автоматически.** Каждый push в `master` запускает GitHub Actions ([.github/workflows/deploy.yml](.github/workflows/deploy.yml)):
сборка и тесты, затем jar передаётся на сервер по SSH. Ключ GitHub Actions на сервере привязан к команде
`/usr/local/bin/art-fly-bot-receive` ([deploy/art-fly-bot-receive.sh](deploy/art-fly-bot-receive.sh)) и не даёт shell:
скрипт только принимает jar, перезапускает службу и, если бот не стартовал, возвращает предыдущую версию.
Запустить выкладку вручную можно кнопкой Run workflow на вкладке Actions.

Однократная настройка:
1. Создать ключ: `ssh-keygen -t ed25519 -N "" -C "github-actions art-fly-bot" -f github_deploy_key`.
2. Добавить его на сервер с принудительной командой:
   `(printf 'command="/usr/local/bin/art-fly-bot-receive",restrict '; cat github_deploy_key.pub) | ssh root@204.77.1.43 'cat >> ~/.ssh/authorized_keys'`.
3. Содержимое `github_deploy_key` сохранить в секрет репозитория `DEPLOY_SSH_KEY`
   (Settings → Secrets and variables → Actions), после чего удалить оба файла ключа.

**Вручную** с машины с root-доступом: `deploy/deploy.sh`. При первой установке он копирует и `application-local.properties`.

Логи: `journalctl -u art-fly-bot -f`, записи: `/opt/art-fly-bot/data/records.csv`.

## Тесты

```bash
./mvnw test
```
