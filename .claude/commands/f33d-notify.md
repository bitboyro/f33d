---
description: Send a notification to the f33d feed
---

Send a message to the f33d notification feed. Use this when you finish a long task, complete a build, finish an analysis, or want to notify the user of something important.

Requires these env vars to be set:
- `F33D_TOKEN` — your API token (printed on f33d startup, or from `create-token`)
- `F33D_URL` — your f33d server URL (default: http://localhost:8080)

```bash
curl -sS \
  -X POST "${F33D_URL:-http://localhost:8080}/api/message" \
  -H "Authorization: Bearer ${F33D_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"message\": $(echo -n "$ARGUMENTS" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')}"
```
