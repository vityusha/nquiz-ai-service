#!/bin/bash
# Ручной деплой на VPS (альтернатива GitHub Actions)
set -e

VPS_HOST="${VPS_HOST:?Set VPS_HOST}"
IMAGE="ghcr.io/your-org/my-service:latest"

echo "Deploying $IMAGE to $VPS_HOST..."
ssh deploy@$VPS_HOST "
  cd /opt/myapp &&
  docker compose pull &&
  docker compose up -d --no-build &&
  echo 'Deploy complete'
"
