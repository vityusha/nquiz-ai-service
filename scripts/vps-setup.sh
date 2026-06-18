#!/usr/bin/env bash
set -euo pipefail

APP_NAME="nquiz-ai-service"
APP_DIR="/opt/${APP_NAME}"
DATA_DIR="/var/lib/${APP_NAME}/data"
DEPLOY_USER="${DEPLOY_USER:-deploy}"
REPO_URL="${REPO_URL:-}"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run as root: sudo $0"
  exit 1
fi

echo "==> Installing Docker (if missing)"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi

echo "==> Creating deploy user"
if ! id "${DEPLOY_USER}" >/dev/null 2>&1; then
  useradd -m -s /bin/bash "${DEPLOY_USER}"
fi
usermod -aG docker "${DEPLOY_USER}"

echo "==> Creating directories"
mkdir -p "${APP_DIR}/config" "${DATA_DIR}"
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "${APP_DIR}" "${DATA_DIR}"

echo "==> Installing compose files"
if [[ -n "${REPO_URL}" ]]; then
  sudo -u "${DEPLOY_USER}" git clone "${REPO_URL}" "${APP_DIR}/repo" || true
  cp "${APP_DIR}/repo/docker-compose.prod.yml" "${APP_DIR}/docker-compose.yml"
  cp "${APP_DIR}/repo/config/application.yml" "${APP_DIR}/config/"
  cp "${APP_DIR}/repo/config/application-prod.yml.example" "${APP_DIR}/config/application-prod.yml"
  cp "${APP_DIR}/repo/.env.example" "${APP_DIR}/.env"
else
  echo "Set REPO_URL to clone project files automatically, or copy them manually to ${APP_DIR}"
fi

cat <<MSG

VPS bootstrap complete.

Next steps:
1. Edit ${APP_DIR}/.env with API keys and DB settings
2. Edit ${APP_DIR}/config/application-prod.yml if needed
3. Log in to GHCR on the server:
     echo <GITHUB_PAT> | docker login ghcr.io -u <github-user> --password-stdin
4. Set IMAGE in ${APP_DIR}/.env, for example:
     IMAGE=ghcr.io/your-org/nquiz-ai-service:latest
5. Start the service:
     cd ${APP_DIR} && docker compose up -d

Point nginx/caddy to 127.0.0.1:8080 for HTTPS termination.
MSG
