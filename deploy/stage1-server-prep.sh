#!/bin/bash
# Stage 1 — Ubuntu server prep ONLY (TankerFlow production).
# Safe: no DB/user creation, no app deploy, no Docker, no public PostgreSQL.
# Run on Lightsail via: bash stage1-server-prep.sh
set -euo pipefail

if [ "$(whoami)" != "ubuntu" ]; then
  echo "Run as ubuntu user (Lightsail default)."
  exit 1
fi

echo ">>> 1. Update Ubuntu packages"
sudo apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y

echo ">>> 2. Create 2 GB swap (helps on 1 GB RAM instances)"
if ! swapon --show | grep -q '/swapfile'; then
  sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo ">>> 3. Install Java 17 JDK"
sudo apt-get install -y openjdk-17-jdk

echo ">>> 4. Install PostgreSQL (local only — do not open 5432 publicly)"
sudo apt-get install -y postgresql postgresql-contrib

echo ">>> 5. Install Nginx"
sudo apt-get install -y nginx

echo ">>> 6. Enable PostgreSQL and Nginx at boot"
sudo systemctl enable postgresql nginx
sudo systemctl start postgresql nginx

echo ">>> 7. Configure UFW (SSH, HTTP, HTTPS — NOT 5432)"
sudo apt-get install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
echo "y" | sudo ufw enable || sudo ufw --force enable

echo ">>> 8. Verification"
bash "$(dirname "$0")/stage1-verify-output.sh"

echo ""
echo "Stage 1 complete. Next: create DB/user locally (stage 2), then deploy Spring Boot JAR."
