# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-04-23

### Added

- Real-time notification feed via HTMX SSE — messages appear instantly in the browser without a page refresh
- Dark, mobile-first web UI rendered server-side with front4j + Tailwind (no build step, no JS framework)
- Local auth mode: auto-generated named API tokens (from `CLIENTS` env var or `TOKENS_FILE`) and auto-generated admin credentials, both printed to stdout on startup
- Keycloak auth mode (`F33D_AUTH_MODE=keycloak`): OIDC login for the web UI, JWT validation for API clients, sender name from `preferred_username` claim
- `POST /api/message` — send a message with a Bearer token; accepts `message` or `text` JSON key
- `GET /api/stream` — SSE endpoint, no auth required
- `GET /api/health` — returns `{status, connected}` with live browser count
- `create-token <name>` subcommand — generates a new token line suitable for appending to `tokens.properties`
- Optional HTTPS with auto-generated self-signed certificate via keytool (`HTTPS_ENABLED=true`)
- `clients/f33d-send.sh` — curl-based shell client, token + URL via args or env vars
- `clients/mcp-server/f33d-mcp.py` — MCP stdio server (Python stdlib only) with `send_message` tool for AI agents
- `.claude/commands/f33d-notify.md` — Claude Code `/f33d-notify` slash command
- Dockerfile + `entrypoint.sh` — single self-contained image, no external services required
- GitHub Actions workflow: builds JAR and publishes Docker image to `ghcr.io` on `v*` tag push
