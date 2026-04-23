#!/bin/bash
# f33d-send.sh — send a message to your f33d feed
#
# Usage:
#   ./f33d-send.sh <token> <message> [url]
#   F33D_TOKEN=xxx F33D_URL=https://... ./f33d-send.sh "Build complete"
#
# Env vars (override positional args):
#   F33D_TOKEN  — auth token
#   F33D_URL    — server base URL (default: http://localhost:8080)

set -euo pipefail

TOKEN="${1:-${F33D_TOKEN:-}}"
MESSAGE="${2:-${F33D_MESSAGE:-}}"
URL="${3:-${F33D_URL:-http://localhost:8080}}"
URL="${URL%/}"

if [ -z "$TOKEN" ]; then
  echo "error: token required (arg 1 or F33D_TOKEN)" >&2; exit 1
fi
if [ -z "$MESSAGE" ]; then
  echo "error: message required (arg 2 or F33D_MESSAGE)" >&2; exit 1
fi

JSON=$(printf '{"message":%s}' "$(printf '%s' "$MESSAGE" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')")

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
