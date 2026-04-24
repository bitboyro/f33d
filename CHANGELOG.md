# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2026-04-24

### Added

- Responsive layout: sidebar collapses to a slide-in drawer on mobile (hamburger toggle + backdrop), narrows to 160px on tablet, full 200px on desktop
- Feed grid tightens to `72px 52px 1fr` on mobile; viewport meta tag added (was missing entirely)

### Fixed

- Suppressed noisy `ERROR` logs from Tomcat for broken-pipe SSE disconnects via a targeted Logback filter (`BrokenPipeFilter`) — the application already handled dead emitters correctly, the logs were pure noise

## [1.1.1] - 2026-04-23

### Fixed

- `SseService.broadcast()` now catches `Exception` (was `IOException`) so `IllegalStateException` from already-completed emitters is also handled cleanly
- Suppressed Tomcat container ERROR logs for broken-pipe events caused by SSE client disconnects (`org.apache.catalina.core.ContainerBase` set to WARN)
- `clients/f33d-send.sh` and `clients/mcp-server/f33d-mcp.py` now send token via `X-Token` header instead of `Authorization: Bearer` — Traefik strips the `Authorization` header before it reaches the app

## [1.1.0] - 2026-04-23

### Added

- Message levels: `POST /api/message` now accepts an optional `level` field (`info` | `success` | `warn` | `error`, default `info`) — each level renders a distinct glyph (`·` `✓` `⚠` `✕`) and color in the feed
- `F33D_LEVEL` env var in `clients/f33d-send.sh` to set level per send
- `level` parameter in `clients/mcp-server/f33d-mcp.py` and `f33d-mcp.sh` MCP tools — agents pick the appropriate level based on outcome
- Level prefix syntax in `.claude/commands/f33d-notify.md`: `/f33d-notify success Build passed`

### Changed

- Rebuilt the feed UI to match the terminal-style design: compact grid rows (SOURCE | TIME | MESSAGE), left sidebar with per-source filtering and levels legend, 48px header with search, pause/resume, and live status dot
- Source names in the feed are now colored by a deterministic palette; message text is colored by level
- Feed now appends new messages at the bottom (newest-last) with auto-scroll
- Removed the "FEED" subtitle from the header — logo is `f33d` only

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
