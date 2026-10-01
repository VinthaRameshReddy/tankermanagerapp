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

# Spring Boot on 1 GB RAM can take 60–90s on cold start
for _ in 1 2 3 4 5 6 8 10 12 15 18 20 25 30; do
  if curl -sf http://127.0.0.1:8080/actuator/health >/dev/null; then
    echo "Deploy OK — API healthy on :8080"
    exit 0
  fi
  sleep 3
done

echo "API did not become healthy:"
sudo journalctl -u tankermanager-api -n 100 --no-pager
exit 1
