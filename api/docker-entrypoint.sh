#!/bin/sh
set -e

if [ -z "$DB_PASSWORD" ]; then
  echo "ERROR: DB_PASSWORD is required"
  exit 1
fi

if [ -z "$PUBLIC_URL" ]; then
  echo "ERROR: PUBLIC_URL is required (the address you open Clarity at)"
  exit 1
fi

PUBLIC_URL="${PUBLIC_URL%/}"

case "$PUBLIC_URL" in
  https://*) SECURE_COOKIES=true ;;
  *) SECURE_COOKIES=false ;;
esac

cat > config.ini <<EOF
[database]
hostname = ${DB_HOST:-postgres}
port = ${DB_PORT:-5432}
username = ${DB_USERNAME:-clarity}
password = ${DB_PASSWORD}
database = ${DB_DATABASE:-clarity}

[web]
port = 8080
secure_cookies = ${SECURE_COOKIES}

[banking]
application_id = ${ENABLE_BANKING_APPLICATION_ID}
key_file = /app/secrets/enable-banking.pem
redirect_url = ${BANKING_REDIRECT_URL:-${PUBLIC_URL}/connect/callback}

[secrets]
key_file = /app/data/clarity.key
EOF

chmod 600 config.ini

exec java --enable-native-access=ALL-UNNAMED -jar api.jar