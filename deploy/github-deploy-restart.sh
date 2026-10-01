#!/bin/bash
# Called by GitHub Actions after JAR + deploy files are uploaded to /opt/tankermanager.
set -euo pipefail

APP_ROOT="/opt/tankermanager"
DEPLOY_DIR="$APP_ROOT/deploy"
API_JAR="$APP_ROOT/api/tanker-manager-1.0.0.jar"
ENV_FILE="$DEPLOY_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE — run stage3-app-setup.sh on the server once."
  exit 1
fi
if [ ! -f "$API_JAR" ]; then
  echo "Missing $API_JAR"
  exit 1
fi

api_healthy() {
  curl -sf http://127.0.0.1:8080/actuator/health >/dev/null && return 0
  curl -sf http://127.0.0.1:8080/v3/api-docs >/dev/null && return 0
  return 1
}

sudo cp "$DEPLOY_DIR/tankermanager-api.service" /etc/systemd/system/tankermanager-api.service
sudo systemctl daemon-reload
sudo systemctl enable tankermanager-api

if [ -f "$DEPLOY_DIR/nginx/tankermanager-host.conf" ]; then
  sudo cp "$DEPLOY_DIR/nginx/tankermanager-host.conf" /etc/nginx/sites-available/tankermanager
  sudo ln -sf /etc/nginx/sites-available/tankermanager /etc/nginx/sites-enabled/tankermanager
  sudo rm -f /etc/nginx/sites-enabled/default
  sudo nginx -t
  sudo systemctl reload nginx
fi

sudo systemctl restart tankermanager-api

echo "Waiting for Spring Boot (1 GB RAM: often 30–90s after restart)..."
sleep 10
for i in $(seq 1 60); do
  if api_healthy; then
    echo "Deploy OK — API healthy on :8080 (after ${i} checks)"
    exit 0
  fi
  sleep 2
done

echo "API did not become healthy:"
sudo journalctl -u tankermanager-api -n 100 --no-pager
exit 1
