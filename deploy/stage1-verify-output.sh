#!/bin/bash
echo "=== JAVA ==="
java -version

echo "=== POSTGRES ==="
sudo systemctl status postgresql --no-pager || true

echo "=== NGINX ==="
sudo systemctl status nginx --no-pager || true

echo "=== SWAP ==="
swapon --show

echo "=== MEMORY ==="
free -h

echo "=== DISK ==="
df -h

echo "=== FIREWALL ==="
sudo ufw status verbose
