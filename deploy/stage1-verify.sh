#!/bin/bash
# Run first after Cursor Remote-SSH connect. Read-only checks.
set -e
echo "=== WHOAMI / HOST ==="
whoami
hostname
lsb_release -a 2>/dev/null || true
echo "=== MEMORY / DISK ==="
free -h
df -h
echo "=== JAVA (may be missing before stage1) ==="
java -version 2>/dev/null || echo "Java not installed yet"
echo "=== PSQL (may be missing before stage1) ==="
psql --version 2>/dev/null || echo "PostgreSQL client not installed yet"
