# NQuiz AI Service

Micronaut service that generates quiz questions with LLM providers for Nibelung NQuiz.

## Stack

- Java 21, Micronaut 4.6, Gradle
- SQLite + Flyway
- Docker / Docker Compose
- GitHub Actions → GHCR → VPS

## API

### Questions

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/questions/generate` | Bearer token | Generate questions via LLM |
| `POST` | `/api/questions/store` | Bearer token | Save questions to local DB |
| `GET` | `/api/questions/get` | None | Get questions from DB (filter by mode/difficulty/type/language/keywords) |

### Capabilities

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/capabilities?lang=en` | Bearer token | List supported languages, types, difficulties, modes, providers |

### Token (user)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/token/info` | Bearer token | Token balance and metadata |

### Token admin

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/admin/tokens/create-user` | Admin Bearer token | Create user token |
| `POST` | `/admin/tokens/create-admin` | Admin Bearer token | Create admin token |
| `POST` | `/admin/tokens/deactivate/{license_no}` | Admin Bearer token | Deactivate token by license |
| `POST` | `/admin/tokens/activate/{license_no}` | Admin Bearer token | Activate token by license |
| `POST` | `/admin/tokens/{license_no_or_token}/topup` | Admin Bearer token | Top up balance |
| `GET` | `/admin/tokens/all` | Admin Bearer token | List all tokens |
| `GET` | `/admin/tokens/stats` | Admin Bearer token | Aggregate stats |
| `GET` | `/admin/tokens/info/{license_no}` | Admin Bearer token | Token info by license |

### Other

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/webhook/payment` | Stripe signature | Payment webhook (outside admin auth) |
| `GET` | `/search` | None | HTML search page for stored questions |
| `GET` | `/health` | None | Health check |

## Local development

### Prerequisites

- JDK 21
- Docker (optional, recommended)

### Run with Docker

```bash
cp .env.example .env
# edit .env — add LLM API keys

./scripts/dev-up.sh
# or manually:
docker compose up -d --build
```

App: http://localhost:8080
Health: http://localhost:8080/health

### Run without Docker

```bash
cp .env.example .env
./gradlew run
```

## Configuration

| File | Purpose |
|------|---------|
| `src/main/resources/application.yml` | Defaults baked into the JAR |
| `config/application.yml` | Shared runtime overrides (DB, health) |
| `config/application-dev.yml` | Local/dev logging |
| `config/application-prod.yml` | Production overrides on VPS (not in git) |
| `.env` | Secrets and `DB_URL` (not in git) |

Copy templates before first run:

```bash
cp .env.example .env
cp config/application-prod.yml.example config/application-prod.yml   # prod only
```

## Notable quirks

- **Dialect.H2 for SQLite:** All `@JdbcRepository` use `Dialect.H2` because Micronaut Data lacks SQLite dialect support. Keep as-is.
- **Rate limit:** Per-IP buckets (`RateLimitFilter`), max 10k tracked IPs: `/api/**` 10 req/min, `/admin/**` 10 req/min (`app.rate-limit.admin-per-minute`).
- **Client IP:** `X-Real-IP` / `X-Forwarded-For` are honoured only when the TCP peer is a trusted proxy (`ClientIpResolver`). Otherwise the socket address is used.
  - `app.trusted-proxies` (env `APP_TRUSTED_PROXIES`): comma-separated IPs/CIDRs, IPv4 and IPv6. Not set → default `127.0.0.0/8,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,fc00::/7` (loopback + private ranges: proxy on the same host or Docker bridge). Empty string → trust nobody. A configured list replaces the default, it is not merged.
  - Invalid entries are skipped with `WARN Ignoring invalid trusted proxy entry: ...` at startup — if you see it, fix the setting: with no valid entry the proxy address is used for every client and all users share one rate-limit bucket.
  - The default lives in Java (`DEFAULT_TRUSTED_PROXIES`), not in a `${app.trusted-proxies:...}` placeholder: Micronaut splits placeholders on every `:`, so IPv6 defaults there collapse (that is how `/7` appeared). Do not move it back.
- **Security headers:** `SecurityHeadersFilter` sets CSP, HSTS, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` on all responses.
- **Errors:** Unhandled exceptions are logged and returned as a generic `500 {"error":"Internal Server Error"}` (`GlobalExceptionHandler`).
- **Top-up limit:** A single top-up (admin or webhook) is capped at 100 000 (`PaymentService.MAX_TOPUP_AMOUNT`).
- **Admin bootstrap:** First run creates an admin token and prints it to stdout when no admin tokens exist.
- **No HTTPS:** Terminated at reverse proxy (Nginx/Caddy). App serves HTTP on :8080.

## Docker image

Multi-stage build in `docker/Dockerfile`:

```bash
docker build -f docker/Dockerfile -t nquiz-ai-service:local .
```

Data is persisted at `/var/lib/nquiz-ai-service/data` inside the container.

## VPS deployment

### 1. Bootstrap the server

On a fresh Ubuntu/Debian VPS:

```bash
curl -fsSL https://raw.githubusercontent.com/YOUR_ORG/nquiz-ai-service/main/scripts/vps-setup.sh | sudo REPO_URL=https://github.com/YOUR_ORG/nquiz-ai-service.git bash
```

Or copy files manually to `/opt/nquiz-ai-service/`:

```
/opt/nquiz-ai-service/
  docker-compose.yml      # copy from docker-compose.prod.yml
  .env
  config/
    application.yml
    application-prod.yml
/var/lib/nquiz-ai-service/data/
```

### 2. Configure secrets

```bash
sudo nano /opt/nquiz-ai-service/.env
sudo nano /opt/nquiz-ai-service/config/application-prod.yml
```

Set at minimum:

```env
DB_URL=jdbc:sqlite:/var/lib/nquiz-ai-service/data/nquiz-ai-service.db
DEEPSEEK_API_KEY=...
```

### 3. Pull image and start

```bash
echo <GITHUB_PAT> | docker login ghcr.io -u <github-user> --password-stdin
cd /opt/nquiz-ai-service
docker compose pull
docker compose up -d
```

The service listens on `127.0.0.1:8080`. Put nginx or Caddy in front for HTTPS.

Example nginx upstream:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 4. GitHub Actions auto-deploy

Add repository secrets:

| Secret | Description |
|--------|-------------|
| `VPS_HOST` | VPS IP or hostname |
| `VPS_SSH_KEY` | Private SSH key for deploy user |
| `VPS_USER` | SSH user (default: `deploy`) |
| `VPS_PORT` | SSH port (default: `22`) |

On push to `main`, CI builds the image, pushes to `ghcr.io/<owner>/nquiz-ai-service`, and runs `docker compose pull && up -d` on the VPS.

Create a `production` environment in GitHub if you use environment protection rules.

## Project layout

```
.
├── config/                  # Runtime config mounted into container
├── docker/
│   └── Dockerfile
├── docker-compose.yml       # Local development
├── docker-compose.prod.yml  # VPS production template
├── scripts/
│   ├── dev-up.sh
│   ├── deploy.sh
│   ├── backup.sh
│   ├── vps-setup.sh
│   └── api_client.py
├── src/
│   └── main/
│       ├── java/
│       └── resources/
└── .github/workflows/
    ├── ci-cd.yml
    └── cleanup.yml
```

## Build & test

```bash
./gradlew test
./gradlew shadowJar
```

Fat JAR output: `build/libs/nquiz-ai-service-*-all.jar`

## License

See [LICENSE.md](LICENSE.md).
