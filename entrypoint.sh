#!/bin/sh
set -e

COMMAND="${1:-serve}"

case "$COMMAND" in
  serve)
    if [ "$HTTPS_ENABLED" = "true" ] && [ ! -f "$KEYSTORE_PATH" ]; then
      echo "f33d: generating self-signed certificate at $KEYSTORE_PATH"
      keytool -genkeypair \
        -alias f33d \
        -keyalg RSA \
        -keysize 2048 \
        -validity 3650 \
        -dname "CN=localhost" \
        -storetype PKCS12 \
        -keystore "$KEYSTORE_PATH" \
        -storepass "$KEYSTORE_PASSWORD" \
        -noprompt
      echo "f33d: certificate generated"
    fi
    exec java \
      -Dserver.port="$PORT" \
      -Dserver.ssl.enabled="$HTTPS_ENABLED" \
      -Dserver.ssl.key-store="$KEYSTORE_PATH" \
      -Dserver.ssl.key-store-password="$KEYSTORE_PASSWORD" \
      -Dserver.ssl.key-store-type=PKCS12 \
      -jar /app/app.jar
    ;;
  create-token)
    if [ -z "$2" ]; then
      echo "Usage: create-token <client-name>" >&2
      exit 1
    fi
    exec java -jar /app/app.jar create-token "$2"
    ;;
  *)
    echo "Unknown command: $COMMAND"
    echo "Usage: serve | create-token <name>"
    exit 1
    ;;
esac
