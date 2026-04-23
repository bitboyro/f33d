#!/usr/bin/env bash
# f33d MCP server (bash, stdio transport) — wraps f33d-send.sh
#
# Requires: bash, curl, python3 (for JSON — same dep as f33d-send.sh)
#
# Config via env vars:
#   F33D_URL    — server base URL  (default: http://localhost:8080)
#   F33D_TOKEN  — auth token
#
# Claude Code / claude.json config:
#   {
#     "mcpServers": {
#       "f33d": {
#         "command": "/path/to/f33d/clients/mcp-server/f33d-mcp.sh",
#         "env": { "F33D_URL": "https://your-f33d-host", "F33D_TOKEN": "your-token" }
#       }
#     }
#   }

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
F33D_SEND="$SCRIPT_DIR/../f33d-send.sh"

F33D_URL="${F33D_URL:-http://localhost:8080}"
F33D_TOKEN="${F33D_TOKEN:-}"

# --- JSON helpers (python3) ------------------------------------------------

json_get() { python3 -c "import json,sys; d=json.loads(sys.argv[1]); print(d$2)" "$1" 2>/dev/null || true; }
json_id()  { python3 -c "
import json, sys
d = json.loads(sys.argv[1])
v = d.get('id')
print('' if v is None else json.dumps(v))
" "$1"; }

respond() { printf '%s\n' "$1"; }

TOOLS_JSON='[{"name":"send_message","description":"Send a notification message to the f33d feed. Use this to notify the user when a task completes, a build finishes, an analysis is done, or anything noteworthy happens. Choose the level that best reflects the outcome: success for completed/passed, warn for partial issues, error for failures, info for neutral updates.","inputSchema":{"type":"object","properties":{"message":{"type":"string","description":"The message text to send"},"level":{"type":"string","enum":["info","success","warn","error"],"description":"Log level controlling the visual indicator in the feed. Defaults to info."}},"required":["message"]}}]'

# --- request handler -------------------------------------------------------

handle() {
  local line="$1"
  local method id message level

  method=$(json_get "$line" ".get('method','')")
  id=$(json_id "$line")

  case "$method" in

    initialize)
      respond "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"f33d\",\"version\":\"1.1.0\"}}}"
      ;;

    tools/list)
      respond "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"tools\":$TOOLS_JSON}}"
      ;;

    tools/call)
      message=$(json_get "$line" "['params']['arguments']['message']")
      level=$(json_get "$line" "['params']['arguments'].get('level','info')")
      if F33D_LEVEL="$level" "$F33D_SEND" "$F33D_TOKEN" "$message" "$F33D_URL" >/dev/null 2>&1; then
        respond "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Message sent.\"}],\"isError\":false}}"
      else
        respond "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Failed to send message.\"}],\"isError\":true}}"
      fi
      ;;

    notifications/initialized)
      ;;  # no response

    *)
      [ -n "$id" ] && respond "{\"jsonrpc\":\"2.0\",\"id\":$id,\"error\":{\"code\":-32601,\"message\":\"Method not found: $method\"}}"
      ;;

  esac
}

# --- main loop -------------------------------------------------------------

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  handle "$line"
done
