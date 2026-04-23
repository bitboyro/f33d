# f33d

A personal notification feed. Scripts, agents, CI pipelines, and AI assistants post messages to it — you read them in one place, in real time, from any device.

---

## How it works

You run one Docker container. It starts up and prints a login password and a set of API tokens to the console. Open the URL in your browser, log in, and you'll see a live feed. Anything that posts a message with a valid token shows up instantly — no refresh needed.

```
┌─ f33d  API tokens ────────────────────────────────────┐
│  script_a    a3f9c2d1e4b5f6a7b8c9d0e1f2a3b4c5d6e7f8   │
│  agent_a     b4e8d3c2f1a0e9d8c7b6a5f4e3d2c1b0a9f8e7   │
│  pipeline_c  c5f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0   │
└───────────────────────────────────────────────────────┘

┌─ f33d  Web UI login ──────────────────────────┐
│  username   admin                             │
│  password   9a2f4c8b1e3d7f0a                  │
└───────────────────────────────────────────────┘
```

The token is how f33d knows who sent each message — the sender name appears next to every entry in the feed.

---

## Quickstart

```bash
docker run -p 8080:8080 ghcr.io/bitboyro/f33d
```

Open [http://localhost:8080](http://localhost:8080), log in with the printed credentials, and send a test message:

```bash
curl -X POST http://localhost:8080/api/message \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "hello from my script"}'
```

---

## Naming your senders

Pass `CLIENTS` to pre-create named tokens. Each name gets its own token and those tokens map back to the name in the feed.

```bash
docker run -p 8080:8080 \
  -e CLIENTS=deploy_bot,morning_report,claude_agent \
  ghcr.io/bitboyro/f33d
```

---

## Keeping tokens between restarts

By default tokens are regenerated every time the container starts. To persist them, mount a volume and set `TOKENS_FILE`:

```bash
docker run -p 8080:8080 \
  -v f33d-data:/data \
  -e TOKENS_FILE=/data/tokens.properties \
  ghcr.io/bitboyro/f33d
```

The file uses a simple `token=name` format:

```
a3f9c2d1e4b5f6a7b8c9d0e1f2a3b4c5d6e7f8=script_a
b4e8d3c2f1a0e9d8c7b6a5f4e3d2c1b0a9f8e7=agent_a
```

To add a new token without restarting:

```bash
docker run --rm ghcr.io/bitboyro/f33d create-token my_new_bot >> /path/to/tokens.properties
```

Then restart the container so it picks up the new entry.

---

## Setting a fixed admin password

```bash
docker run -p 8080:8080 \
  -e ADMIN_USER=roman \
  -e ADMIN_PASSWORD=mypassword \
  ghcr.io/bitboyro/f33d
```

---

## Message levels

Every message has a level that controls the visual indicator shown in the feed:

| Level | Glyph | Color | When to use |
|---|---|---|---|
| `info` | `·` | white | neutral updates, heartbeats, progress notes |
| `success` | `✓` | green | completed tasks, passed builds, successful deploys |
| `warn` | `⚠` | yellow | partial failures, coverage drops, rate limits |
| `error` | `✕` | red | hard failures, crashes, timeouts |

`info` is the default when `level` is omitted.

---

## Sending messages

### curl

```bash
curl -X POST https://your-f33d-host/api/message \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "Build finished in 42s", "level": "success"}'
```

The `level` field is optional and defaults to `info`.

### Shell script

```bash
F33D_TOKEN=<token> F33D_URL=https://your-f33d-host \
  ./clients/f33d-send.sh "Deployment complete"
```

Set `F33D_LEVEL` to override the default level:

```bash
F33D_TOKEN=<token> F33D_LEVEL=success ./clients/f33d-send.sh "Deploy passed"
F33D_TOKEN=<token> F33D_LEVEL=error   ./clients/f33d-send.sh "Deploy failed"
```

Or with positional args (`<token> <message> [level] [url]`):

```bash
./clients/f33d-send.sh <token> "Deployment complete"
./clients/f33d-send.sh <token> "Deploy passed" success
./clients/f33d-send.sh <token> "Deploy failed" error https://your-f33d-host
```

### Claude Code slash command

If you have f33d running and `F33D_TOKEN` / `F33D_URL` set in your environment, you can notify yourself from inside any Claude Code session. Prefix the message with a level keyword:

```
/f33d-notify success Build passed — 2m 14s
/f33d-notify warn Coverage dropped below threshold
/f33d-notify error Deploy failed — pod OOMKilled
/f33d-notify Analysis complete — 3 issues found
```

The level prefix is optional; messages without one default to `info`.

### MCP server (for AI agents)

The `clients/mcp-server/f33d-mcp.py` file is a Python MCP server (no pip required — stdlib only). Add it to your Claude Desktop or claude.json config:

```json
{
  "mcpServers": {
    "f33d": {
      "command": "python3",
      "args": ["/path/to/f33d/clients/mcp-server/f33d-mcp.py"],
      "env": {
        "F33D_URL": "https://your-f33d-host",
        "F33D_TOKEN": "your-token"
      }
    }
  }
}
```

The agent gains a `send_message` tool with a `level` parameter it can set based on outcome. The tool description instructs the model to choose `success` for completions, `warn` for partial issues, and `error` for failures.

---

## AI assistant setup

### Claude Code

Two options — use whichever fits your workflow.

**Option A: `/f33d-notify` slash command**

Copy the command file into your project's `.claude/commands/` directory:

```bash
mkdir -p .claude/commands
cp /path/to/f33d/clients/f33d-send.sh .  # optional helper
curl -o .claude/commands/f33d-notify.md \
  https://raw.githubusercontent.com/bitboyro/f33d/main/.claude/commands/f33d-notify.md
```

Or create `.claude/commands/f33d-notify.md` manually:

````markdown
---
description: Send a notification to the f33d feed
---

Send a message to the f33d notification feed.

Requires: F33D_TOKEN and F33D_URL env vars.

```bash
curl -sS \
  -X POST "${F33D_URL:-http://localhost:8080}/api/message" \
  -H "Authorization: Bearer ${F33D_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"message\": $(echo -n "$ARGUMENTS" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')}"
```
````

Then set your env vars and use it in any session:

```bash
export F33D_TOKEN=your-token
export F33D_URL=https://your-f33d-host
```

```
/f33d-notify Build finished — 3 warnings
```

**Option B: MCP server (gives Claude a `send_message` tool it can call autonomously)**

Add to `~/.claude/claude.json` for global access, or `.claude/mcp.json` inside a project:

```json
{
  "mcpServers": {
    "f33d": {
      "command": "python3",
      "args": ["/path/to/f33d/clients/mcp-server/f33d-mcp.py"],
      "env": {
        "F33D_URL": "https://your-f33d-host",
        "F33D_TOKEN": "your-token"
      }
    }
  }
}
```

Claude will automatically call `send_message` when it finishes long tasks.

---

### GitHub Copilot (VS Code)

GitHub Copilot Chat supports MCP servers in Agent mode. Create `.vscode/mcp.json` in your project:

```json
{
  "servers": {
    "f33d": {
      "command": "python3",
      "args": ["/path/to/f33d/clients/mcp-server/f33d-mcp.py"],
      "env": {
        "F33D_URL": "https://your-f33d-host",
        "F33D_TOKEN": "your-token"
      }
    }
  }
}
```

Then in VS Code:

1. Open GitHub Copilot Chat and switch to **Agent mode** (the `@` dropdown → select an agent, or use `#` to reference tools).
2. VS Code will prompt you to enable the f33d MCP server — click **Allow**.
3. Copilot can now call `send_message` during long agentic tasks.

To apply it globally instead of per-project, add the same block under `"mcp"` → `"servers"` in your VS Code user `settings.json`.

---

### What to tell your agent

Add this to your project's `CLAUDE.md` (or equivalent agent instructions) so the agent knows when and how to notify you.

**Slash command variant** — for Claude Code with the `/f33d-notify` command installed:

```markdown
## Notifications
When you finish a task, send a notification: `/f33d-notify <summary>`
```

**MCP variant** — for any agent with the f33d MCP server connected:

```markdown
## Notifications
You have access to a `send_message` tool (f33d MCP server).
Call it when: a build finishes, an analysis completes, or anything the user would want to know about while away from the terminal.
Keep messages short — one sentence with the outcome.
Set `level` based on the outcome: success=passed/complete, warn=partial issue, error=failure, info=neutral update.
Never include secrets, credentials, API keys, passwords, IP addresses, or any personally identifiable information in messages unless explicitly instructed to do so.
```

---

## HTTPS

To enable HTTPS with a self-signed certificate:

```bash
docker run -p 8443:8443 \
  -e PORT=8443 \
  -e HTTPS_ENABLED=true \
  -e KEYSTORE_PATH=/data/f33d.p12 \
  -v f33d-data:/data \
  ghcr.io/bitboyro/f33d
```

The certificate is auto-generated on first start and reused on subsequent starts.

---

## Keycloak / SSO

If you're running a Keycloak instance, f33d can delegate all auth to it. The web UI gets OIDC login and API clients send JWTs instead of static tokens. The sender name is taken from the `preferred_username` claim.

```bash
docker run -p 8080:8080 \
  -e F33D_AUTH_MODE=keycloak \
  -e KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/my-realm \
  -e KEYCLOAK_CLIENT_ID=f33d \
  -e KEYCLOAK_CLIENT_SECRET=xxx \
  ghcr.io/bitboyro/f33d
```

Register `f33d` in Keycloak as an OIDC client with `standardFlow` enabled and redirect URI `https://your-f33d-host/login/oauth2/code/keycloak`.

---

## Health check

```bash
curl https://your-f33d-host/api/health
# {"status":"ok","connected":2}
```

`connected` is the number of browsers currently watching the live feed.

---

## Building from source

Requires Java 21 and Maven.

```bash
# Build the JAR
mvn package -DskipTests

# Run locally
java -jar target/f33d.jar
```

```bash
# Build the Docker image
docker build -t f33d .

# Run it
docker run -p 8080:8080 f33d
```

The GitHub Actions workflow ([.github/workflows/publish.yml](.github/workflows/publish.yml)) builds and pushes to `ghcr.io` automatically on every `v*` tag push:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This produces:
- `ghcr.io/<owner>/f33d:1.0.0`
- `ghcr.io/<owner>/f33d:1.0`
- `ghcr.io/<owner>/f33d:latest`

---

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `HTTPS_ENABLED` | `false` | Enable TLS |
| `KEYSTORE_PATH` | — | Path to PKCS12 keystore (auto-created if missing) |
| `KEYSTORE_PASSWORD` | `f33d-secret` | Keystore password |
| `F33D_AUTH_MODE` | `local` | `local` or `keycloak` |
| `CLIENTS` | — | Comma-separated names to auto-generate tokens for |
| `TOKENS_FILE` | — | Path to a `token=name` properties file |
| `ADMIN_USER` | `admin` | Web UI username |
| `ADMIN_PASSWORD` | *(auto-generated)* | Web UI password |
| `KEYCLOAK_ISSUER_URI` | — | Keycloak realm issuer URL (keycloak mode) |
| `KEYCLOAK_CLIENT_ID` | `f33d` | OIDC client ID (keycloak mode) |
| `KEYCLOAK_CLIENT_SECRET` | — | OIDC client secret (keycloak mode) |
