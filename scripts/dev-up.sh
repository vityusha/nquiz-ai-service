#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Created .env from .env.example — fill in your API keys before starting."
fi

mkdir -p data config
docker compose up -d --build

echo "Service is starting on http://localhost:8080"
echo "Health check: curl -fsS http://localhost:8080/health"
