---
description: Send a notification to the f33d feed
---

Send a message to the f33d notification feed. Use this when you finish a long task, complete a build, finish an analysis, or want to notify the user of something important.

Requires these env vars to be set:
- `F33D_TOKEN` — your API token (printed on f33d startup, or from `create-token`)
- `F33D_URL` — your f33d server URL (default: http://localhost:8080)

Optionally prefix the message with a level keyword to control the visual indicator:
- `/f33d-notify success Build passed in 2m 14s`
- `/f33d-notify warn Test coverage dropped to 78%`
- `/f33d-notify error Deploy failed — pod OOMKilled`
- `/f33d-notify Analysis complete` (defaults to info)

```bash
# Parse optional level prefix: /f33d-notify [info|success|warn|error] <message>
FIRST="${ARGUMENTS%% *}"
REST="${ARGUMENTS#* }"
case "$FIRST" in
  info|success|warn|error)
    LEVEL="$FIRST"
    MESSAGE="$REST"
    ;;
  *)
    LEVEL="${F33D_LEVEL:-info}"
    MESSAGE="$ARGUMENTS"
    ;;
esac

curl -sS \
  -X POST "${F33D_URL:-http://localhost:8080}/api/message" \
  -H "Authorization: Bearer ${F33D_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"message\": $(echo -n "$MESSAGE" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'), \"level\": \"$LEVEL\"}"
```
