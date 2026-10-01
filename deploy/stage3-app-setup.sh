#!/bin/bash
# Stage 3 — one-time: /opt/tankermanager layout, .env, systemd, host Nginx.
# Prerequisite: stage1-server-prep.sh + stage2-create-local-db.sh
#
#   export TANKER_DB_PASSWORD='your-db-password'
#   export JWT_SECRET='long-random-string'   # optional; generated if unset
#   bash stage3-app-setup.sh
set -euo pipefail

APP_ROOT="/opt/tankermanager"
DEPLOY_DIR="$APP_ROOT/deploy"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [ "$(whoami)" != "ubuntu" ]; then
  echo "Run as ubuntu."
  exit 1
fi

if [ -z "${TANKER_DB_PASSWORD:-}" ]; then
  echo "Set TANKER_DB_PASSWORD (same as stage 2)."
  exit 1
fi

JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48)}"

sudo mkdir -p "$APP_ROOT/api" "$DEPLOY_DIR/nginx"
sudo chown -R ubuntu:ubuntu "$APP_ROOT"

ENC_PASS="$(TANKER_DB_PASSWORD="$TANKER_DB_PASSWORD" python3 -c 'import os, urllib.parse; print(urllib.parse.quote(os.environ["TANKER_DB_PASSWORD"], safe=""))')"

cat > "$DEPLOY_DIR/.env" <<EOF
DATABASE_URL=postgresql://tanker:${ENC_PASS}@localhost:5432/tankermanager
DATABASE_SSL_MODE=disable
JWT_SECRET=${JWT_SECRET}
EOF
chmod 600 "$DEPLOY_DIR/.env"

cp "$REPO_DIR/deploy/tankermanager-api.service" "$DEPLOY_DIR/"
cp "$REPO_DIR/deploy/github-deploy-restart.sh" "$DEPLOY_DIR/"
cp "$REPO_DIR/deploy/nginx/tankermanager-host.conf" "$DEPLOY_DIR/nginx/"
chmod +x "$DEPLOY_DIR/github-deploy-restart.sh"

sudo cp "$DEPLOY_DIR/tankermanager-api.service" /etc/systemd/system/tankermanager-api.service
sudo cp "$DEPLOY_DIR/nginx/tankermanager-host.conf" /etc/nginx/sites-available/tankermanager
sudo ln -sf /etc/nginx/sites-available/tankermanager /etc/nginx/sites-enabled/tankermanager
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
sudo systemctl daemon-reload

echo ""
echo "Stage 3 complete."
echo "  Env file: $DEPLOY_DIR/.env"
echo "  JWT_SECRET saved in .env (keep it; do not commit)."
echo ""
echo "Next — GitHub repo → Settings → Secrets → Actions:"
echo "  LIGHTSAIL_HOST     = your static IP"
echo "  LIGHTSAIL_USER     = ubuntu"
echo "  LIGHTSAIL_SSH_KEY  = full .pem private key"
echo ""
echo "Then push to main (backend change) or run Actions → Deploy API to Lightsail."
echo "First deploy uploads the JAR; until then API service may fail to start (expected)."
