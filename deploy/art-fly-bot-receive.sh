#!/usr/bin/env bash
# Принимает jar бота на stdin, устанавливает его и перезапускает службу.
# Если бот не стартовал за 120 секунд, возвращает предыдущую версию.
#
# Устанавливается на сервер в /usr/local/bin/art-fly-bot-receive и вызывается
# как принудительная команда ключа GitHub Actions в /root/.ssh/authorized_keys:
#   command="/usr/local/bin/art-fly-bot-receive",restrict ssh-ed25519 AAAA... github-actions
# Поэтому этот ключ не даёт shell: любая команда ssh запускает только этот скрипт.
set -euo pipefail

APP_DIR=/opt/art-fly-bot
JAR="$APP_DIR/art-fly-bot.jar"
SERVICE=art-fly-bot
MAX_BYTES=$((300 * 1024 * 1024))
START_TIMEOUT=120

exec 9>/run/art-fly-bot-deploy.lock
flock -n 9 || { echo "Другая выкладка ещё идёт" >&2; exit 1; }

tmp=$(mktemp -p "$APP_DIR" .upload.XXXXXX)
trap 'rm -f "$tmp"' EXIT

head -c $((MAX_BYTES + 1)) > "$tmp"
size=$(stat -c %s "$tmp")
if (( size > MAX_BYTES )); then
    echo "jar больше $MAX_BYTES байт" >&2; exit 1
fi
if (( size < 1024 * 1024 )) || ! unzip -tqq "$tmp" >/dev/null 2>&1; then
    echo "Получен не jar (${size} байт)" >&2; exit 1
fi
echo "Получен jar: $size байт, sha256 $(sha256sum "$tmp" | cut -c1-16)"

# Журнал службы после перезапуска: курсор берётся после restart, чтобы не видеть остановку старого процесса
service_log() {
    journalctl -u "$SERVICE" --after-cursor "$1" --no-pager -o cat
}

restart_service() {
    systemctl restart "$SERVICE"
    journalctl -u "$SERVICE" -n 1 --show-cursor --no-pager -o cat | sed -n 's/^-- cursor: //p'
}

wait_started() {
    local cursor=$1
    local log
    for _ in $(seq 1 "$START_TIMEOUT"); do
        log=$(service_log "$cursor")
        if grep -q 'Started ArtFlyBotApplication' <<<"$log"; then
            return 0
        fi
        # Упавший процесс systemd перезапускает сам (Restart=on-failure), поэтому ловим первый выход
        if systemctl is-failed --quiet "$SERVICE" || grep -q 'Main process exited' <<<"$log"; then
            return 1
        fi
        sleep 1
    done
    return 1
}

[[ -f "$JAR" ]] && cp -p "$JAR" "$JAR.prev"
install -o root -g artfly -m 640 "$tmp" "$JAR"

cursor=$(restart_service)
if wait_started "$cursor"; then
    service_log "$cursor" | grep -E 'Started ArtFlyBotApplication|WARN|ERROR' | cut -c1-200
    echo "Выкладка завершена"
    exit 0
fi

echo "Бот не запустился, лог:" >&2
service_log "$cursor" | tail -n 40 >&2
if [[ -f "$JAR.prev" ]]; then
    echo "Возвращаю предыдущую версию" >&2
    install -o root -g artfly -m 640 "$JAR.prev" "$JAR"
    cursor=$(restart_service)
    wait_started "$cursor" && echo "Предыдущая версия запущена" >&2 || echo "Предыдущая версия тоже не запустилась" >&2
fi
exit 1
