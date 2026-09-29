#!/bin/bash
# Run ON the Lightsail Ubuntu instance (as ubuntu).
#   export DATABASE_URL='postgresql://user:pass@host:5432/tankermanager'
#   export JWT_SECRET='...'
#   bash bootstrap-on-server.sh
set -euo pipefail

APP_DIR="/opt/tankermanager"
REPO_URL="https://github.com/VinthaRameshReddy/tankermanagerapp.git"

install_docker() {
  bash "$APP_DIR/deploy/lightsail-install-docker.sh"
}

if ! command -v docker >/dev/null 2>&1; then
  sudo mkdir -p "$APP_DIR"
  sudo chown -R "$USER:$USER" "$APP_DIR"
  if [ ! -d "$APP_DIR/.git" ]; then
    git clone "$REPO_URL" "$APP_DIR"
  fi
  install_docker
  echo ">>> Docker installed. Log out, SSH back in, set DATABASE_URL + JWT_SECRET, run this script again."
  exit 0
fi

sudo mkdir -p "$APP_DIR"
sudo chown -R "$USER:$USER" "$APP_DIR"

if [ ! -d "$APP_DIR/.git" ]; then
  git clone "$REPO_URL" "$APP_DIR"
else
  cd "$APP_DIR" && git pull origin main
fi

cd "$APP_DIR/deploy"

if [ -z "${DATABASE_URL:-}" ] || [ -z "${JWT_SECRET:-}" ]; then
  echo "Missing env. Example:"
  echo "  export DATABASE_URL='postgresql://USER:PASS@endpoint:5432/tankermanager'"
  echo "  export JWT_SECRET=\$(openssl rand -base64 48)"
  exit 1
fi

cat > .env <<EOF
DATABASE_URL=${DATABASE_URL}
JWT_SECRET=${JWT_SECRET}
EOF

echo "Building and starting..."
docker compose -f docker-compose.prod.yml build --no-cache api
docker compose -f docker-compose.prod.yml up -d

sleep 30
docker compose -f docker-compose.prod.yml logs --tail=50 api
echo ""
echo "Test: curl -s http://YOUR_STATIC_IP/api/auth/login -H 'Content-Type: application/json' -d '{\"phone\":\"9999999999\",\"password\":\"Admin@123\"}'"
