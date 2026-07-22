# nquiz-ai-service

## Quick start

```bash
cp .env.example .env        # fill in LLM API keys
./scripts/dev-up.sh          # docker compose up --build
# or without Docker:
./gradlew run
```

## Build & test

```bash
./gradlew test               # JUnit 5 + JaCoCo (coverage report in build/reports/)
./gradlew shadowJar          # fat JAR at build/libs/*-all.jar
./gradlew shadowJar -x test  # skip tests for faster rebuild
```

No lint or typecheck tasks exist.

## Architecture

- **Framework:** Micronaut 4.6 (Netty runtime), Java 21
- **Entrypoint:** `com.lainlab.Application` (micronaut application plugin)
- **DB:** SQLite + Flyway (repos use `Dialect.H2` — intentional workaround, do not "fix")
- **Config files:** `config/application.yml` (shared), `config/application-dev.yml` (local), `config/application-prod.yml` (VPS, not in git)
- **Secrets:** `.env` (not in git) — LLM API keys + `DB_URL` override
- **Runtime env:** `MICRONAUT_ENVIRONMENTS=dev|prod`, `MICRONAUT_CONFIG_FILES=...`

## Test quirks

- `maxParallelForks = 1` — tests must run sequentially
- API key env vars + `PAYMENT_WEBHOOK_SECRET` are hardcoded in `build.gradle` `test { environment ... }` — tests never hit real LLMs
- H2 in-memory DB for tests (not SQLite)
- JaCoCo excludes: `config/**`, `model/**`, `dto/**`, `db/**`

## Notable quirks

- **Dialect.H2 for SQLite:** All `@JdbcRepository` use `Dialect.H2` because Micronaut Data lacks SQLite dialect support. Keep as-is.
- **Rate limit:** Per-IP 10 req/min bucket (`RateLimitFilter`), max 10k tracked IPs.
- **No HTTPS:** Terminated at reverse proxy (Nginx/Caddy). App serves HTTP on :8080.
- **Admin bootstrap:** First run creates an admin token and prints it to stdout when no admin tokens exist.
- **Dockerfile:** Pre-built JAR expected at `build/libs/*-all.jar` — build locally first, then `docker build -f docker/Dockerfile`.

## CI/CD

GitHub Actions pushes to `main`:

1. `./gradlew test`
2. `./gradlew shadowJar -x test`
3. Build & push Docker image to `ghcr.io/<owner>/nquiz-ai-service` (tagged `:${sha}` + `:latest`)
4. SSH to VPS → `docker compose pull && up -d --remove-orphans`

Secrets needed: `VPS_SSH_KEY`, `VPS_HOST`, `VPS_USER`, `VPS_PORT`.
