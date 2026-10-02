#!/usr/bin/env bash
# Сборка и выкладка бота на VPS.
# Использование: deploy/deploy.sh [user@host]   (по умолчанию root@204.77.1.43)
# Секреты (application-local.properties) копируются только при первой установке,
# если на сервере их ещё нет. data/records.csv на сервере никогда не перезаписывается.
set -euo pipefail

HOST="${1:-root@204.77.1.43}"
APP_DIR=/opt/art-fly-bot
cd "$(dirname "$0")/.."

./mvnw -q clean package
JAR=$(ls target/art-fly-bot-*.jar | grep -v plain | head -1)

ssh "$HOST" "set -e
  command -v java >/dev/null || { apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jre-headless; }
  id artfly >/dev/null 2>&1 || useradd --system --home-dir $APP_DIR --shell /usr/sbin/nologin artfly
  mkdir -p $APP_DIR/data"

scp -q "$JAR" "$HOST:$APP_DIR/art-fly-bot.jar.new"
scp -q deploy/art-fly-bot.service "$HOST:/etc/systemd/system/art-fly-bot.service"
ssh "$HOST" "test -f $APP_DIR/data/masters.csv" || scp -q data/masters.csv "$HOST:$APP_DIR/data/masters.csv"
if ! ssh "$HOST" "test -f $APP_DIR/application-local.properties"; then
  scp -q application-local.properties "$HOST:$APP_DIR/application-local.properties"
fi

ssh "$HOST" "set -e
  mv $APP_DIR/art-fly-bot.jar.new $APP_DIR/art-fly-bot.jar
  touch $APP_DIR/data/records.csv
  chown -R root:artfly $APP_DIR && chmod 750 $APP_DIR
  chmod 640 $APP_DIR/art-fly-bot.jar $APP_DIR/application-local.properties
  chown -R artfly:artfly $APP_DIR/data
  systemctl daemon-reload
  systemctl enable --quiet art-fly-bot
  systemctl restart art-fly-bot
  systemctl --no-pager --lines=0 status art-fly-bot"
