#!/bin/bash
# Stage 2 — Create PostgreSQL database on THIS server only (localhost).
# Prerequisite: stage1-server-prep.sh completed.
# Usage:
#   export TANKER_DB_PASSWORD='your-strong-password'
#   bash stage2-create-local-db.sh
set -euo pipefail

DB_NAME="tankermanager"
DB_USER="tanker"

if [ -z "${TANKER_DB_PASSWORD:-}" ]; then
  echo "Set TANKER_DB_PASSWORD first, e.g.:"
  echo "  export TANKER_DB_PASSWORD='...'"
  exit 1
fi

echo ">>> Creating role and database (local access only)"
sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${DB_USER}') THEN
    CREATE ROLE ${DB_USER} LOGIN PASSWORD '${TANKER_DB_PASSWORD}';
  ELSE
    ALTER ROLE ${DB_USER} WITH PASSWORD '${TANKER_DB_PASSWORD}';
  END IF;
END
\$\$;
SELECT 'CREATE DATABASE ${DB_NAME} OWNER ${DB_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}')\gexec
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
SQL

echo ">>> Confirm PostgreSQL listens on localhost (default on Ubuntu)"
sudo -u postgres psql -c "SHOW listen_addresses;"

echo ""
echo "Use in /opt/tankermanager/deploy/.env or systemd:"
echo "DATABASE_URL=postgresql://${DB_USER}:URL_ENCODE_PASSWORD@localhost:5432/${DB_NAME}"
echo "DATABASE_SSL_MODE=disable"
echo ""
echo "Test:"
echo "  PGPASSWORD='...' psql -h localhost -U ${DB_USER} -d ${DB_NAME} -c 'SELECT 1'"
