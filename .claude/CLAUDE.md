# CLAUDE.md — bitboy.ro platform workspace

> This file tells Claude Code how to work across the bitboy.ro scaffold platform.
> It is the top-level guide for the multi-project workspace.

## What this workspace is

This is the monorepo workspace for the **bitboy.ro scaffold platform** — a self-hosted multi-service platform deployed on a Hetzner VM behind Traefik, with Keycloak auth, Prometheus+Loki observability, and GitLab CI/CD.

### Projects in this workspace

| Project | What it is | Language | Status |
|---|---|---|---|
| `scaffold` | Infrastructure-only repo: docker-compose, Traefik, Prometheus, Loki, Promtail, CI templates, platform standards | YAML/Shell | Active |
| `ork` | Platform orchestration app — service health dashboard, user/role management via Keycloak admin API, inference backend lifecycle control (Docker socket), chat UI for testing inference backends | Java 21 / Spring Boot 3.4 | Active |
| `covrigi` | Code quality analytics — parses Java source into dependency graphs, overlays JaCoCo coverage, LLM-powered speculative analysis, browser UI (front4j + HTMX + D3.js) | Java 21 / Spring Boot 3.3 | Active |
| `infer` | Inference platform — JWT-gated MCP clients for vLLM, llama.cpp, OpenAI; MCP server with Redis job store + Kafka async; Docker container lifecycle controller | Java 21 / Spring Boot | Active |
| `sc-core` | Shared Spring Boot auto-configuration library — `X-SC-ID` request correlation across HTTP and Kafka, `ErrorResponse` DTO | Java 21 / Spring Boot | Active |
| `agentguild` | **Not part of scaffold** — ignore in platform context | Java | Separate |

### How they relate

```
scaffold (infrastructure)
  ├── docker-compose.yml — orchestrates all services
  ├── standards/ — platform contracts (CLAUDE.md template, LOGGING, PATTERNS, etc.)
  └── ci/ — reusable GitLab CI templates

sc-core (shared library)
  └── consumed by: ork, covrigi, infer (all Java services)

ork (platform control plane, port 8080)
  ├── Dashboard — health status for all platform layers
  ├── User/role management — Keycloak admin API
  ├── Inference backend lifecycle — Docker socket control (vllm, llamacpp)
  ├── Chat UI — test inference via vLLM/LlamaCpp/OpenAI
  └── Postgres (scaffold-db), Keycloak, Docker socket, infer-* clients

covrigi (standalone service)
  └── Postgres + Memgraph, port 7070/7443, Keycloak optional

infer (multi-module)
  ├── mcp-client-vllm (8090)
  ├── mcp-client-llamacpp (8091)
  ├── mcp-client-openai (8092)
  ├── mcp-server-inference (8100, internal only)
  └── inference-controller (8093, Docker API)
```

## Platform-wide rules

### UI rendering
- **front4j only** — no React, no Thymeleaf, no HTML templates. All markup built programmatically in Java using `html.H.*` fluent API + HTMX + Tailwind CSS.
- See `.claude/skills/front4j/SKILL.md` for the full front4j reference.

### Request correlation (sc_id)
- Every request carries `X-SC-ID` header, propagated by sc-core across HTTP and Kafka.
- Entry-point services generate sc_id if absent; internal services and MCP servers/clients never generate.
- All services log `scId` in structured JSON via MDC.

### Logging
- Structured JSON to stdout. Promtail scrapes Docker logs → Loki.
- Required fields: `timestamp`, `level`, `service`, `version`, `scId`, `class`, `message`.
- Never `System.out.println`, never custom log formats.

### Auth
- Keycloak OIDC (`bitboy-internal` realm). JWT validation via `JWT_ISSUER_URI` env var.
- No custom auth implementations.

### Config
- `application.yml` + env vars. `@ConfigurationProperties` only — no `@Value` in business logic.
- Secrets via env vars, never committed.

### Error responses
- Standard shape: `{ "code": "...", "message": "...", "detail": "..." }` via sc-core `ErrorResponse`.

### Health & metrics
- Java: `/actuator/health` + `/actuator/prometheus` via Spring Actuator + Micrometer.
- Python: `/health` + `/metrics`.

### Networking
- Services behind Traefik reverse proxy with TLS (Let's Encrypt).
- Isolated Docker networks per service DB — no cross-service DB access.
- Inter-service communication through Traefik HTTPS or Kafka.

### CI/CD
- GitLab CI with reusable templates from `scaffold/ci/`.
- Tag push → auto publish + deploy. Regular commit → manual deploy.
- Images pushed to GitLab Container Registry.

### Documentation contract
Every service MUST maintain:
- `README.md` — what it is, how to run, env vars
- `CHANGELOG.md` — Keep a Changelog format
- `SKILLS.md` — plain-language capability descriptions
- `ARCHITECTURE.md` — decisions, patterns, dependencies
- `CLAUDE.md` — LLM agent instructions (from `scaffold/standards/CLAUDE.md.template`)

Update `SKILLS.md` and `CHANGELOG.md` in the same commit when changing behaviour.

## Build commands

```bash
# sc-core (library — install first for downstream dev)
cd sc-core && ./mvnw clean install

# ork (needs postgres + keycloak running)
cd ork && ./mvnw clean verify
cd ork && ./mvnw spring-boot:run

# covrigi
cd covrigi && mvn clean verify
cd covrigi && COVRIGI_PASSWORD=changeme mvn spring-boot:run -Dspring-boot.run.arguments="--server.ssl.enabled=false --server.port=7070"

# infer (multi-module — sc-core must be installed first)
cd infer && ./mvnw clean verify
cd infer && ./mvnw spring-boot:run -pl mcp-client-vllm
```

## What NOT to do

- Do not add React, Thymeleaf, or any frontend framework — UI is front4j only
- Do not roll custom auth — use Keycloak OIDC
- Do not log to files — stdout only
- Do not manage TLS — Traefik terminates SSL
- Do not hardcode service URLs — use environment variables
- Do not expose ports directly — Traefik routes via Docker labels
- Do not generate sc_id in MCP servers or MCP clients
- Do not put `traceId` in MDC manually — use sc-core
- Do not access other services' databases directly
