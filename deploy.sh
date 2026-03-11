#!/bin/bash

# SOSO Server 수동 배포 스크립트
# GitHub Actions가 자동으로 배포하지만, 수동 배포가 필요할 때 사용
# 사용법: ./deploy.sh

set -e  # 에러 발생시 스크립트 중단

echo "🐳 Pulling latest image from GHCR..."
docker compose pull api

echo "🚀 Deploying with Docker Compose..."
docker compose up -d --no-deps --wait --wait-timeout 180 api

echo "🌐 Reloading Caddy config..."
docker exec soso-proxy caddy reload --config /etc/caddy/Caddyfile --force 2>/dev/null || true

echo "🧹 Cleaning old images..."
docker image prune -f --filter "until=24h" || true

echo "📋 Checking container status..."
docker compose ps api

echo "✅ Deployment completed!"
