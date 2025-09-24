#!/bin/bash

# SOSO Server 자동 배포 스크립트
# 사용법: ./deploy.sh

set -e  # 에러 발생시 스크립트 중단

echo "🔨 Building new Docker image..."
docker build -t localTest/soso-server:latest .

echo "🚀 Deploying with Docker Compose..."
docker compose up -d

echo "📋 Checking container status..."
docker ps | grep soso

echo "✅ Deployment completed!"
echo "🌐 Your app is running at: http://localhost:8080"
echo ""
echo "💡 Tip: Use 'docker logs soso-app -f' to see live logs"