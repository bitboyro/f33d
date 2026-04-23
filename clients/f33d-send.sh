#!/bin/bash
# f33d-send.sh — send a message to your f33d feed
#
# Usage:
#   ./f33d-send.sh <token> <message> [level] [url]
#   F33D_TOKEN=xxx F33D_URL=https://... ./f33d-send.sh "Build complete"
#   ./f33d-send.sh <token> "Build passed" success
#   F33D_TOKEN=xxx F33D_LEVEL=success ./f33d-send.sh "Build passed"
#
# Env vars (override positional args):
#   F33D_TOKEN  — auth token
#   F33D_URL    — server base URL (default: http://localhost:8080)
#   F33D_LEVEL  — log level: info | success | warn | error (default: info)

set -euo pipefail

TOKEN="${1:-${F33D_TOKEN:-}}"
MESSAGE="${2:-${F33D_MESSAGE:-}}"
LEVEL="${3:-${F33D_LEVEL:-info}}"
URL="${4:-${F33D_URL:-http://localhost:8080}}"
URL="${URL%/}"

if [ -z "$TOKEN" ]; then
  echo "error: token required (arg 1 or F33D_TOKEN)" >&2; exit 1
fi
if [ -z "$MESSAGE" ]; then
  echo "error: message required (arg 2 or F33D_MESSAGE)" >&2; exit 1
fi

case "$LEVEL" in
  info|success|warn|error) ;;
  *) echo "error: level must be info, success, warn, or error (got: $LEVEL)" >&2; exit 1 ;;
esac

JSON=$(printf '{"message":%s,"level":"%s"}' \
  "$(printf '%s' "$MESSAGE" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')" \
  "$LEVEL")

HTTP_CODE=$(curl -sS -o /tmp/f33d_response -w "%{http_code}" \
  -X POST "$URL/api/message" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$JSON")
BODY=$(cat /tmp/f33d_response)

if [ "$HTTP_CODE" = "200" ]; then
  echo "✓ sent"
else
  echo "error ($HTTP_CODE): $BODY" >&2; exit 1
fi
