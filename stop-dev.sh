#!/bin/bash
# 로컬 개발 환경 중지 스크립트

echo "🛑 SOSO 로컬 개발 환경을 중지합니다..."

# DB/Redis 컨테이너 중지
docker compose -f compose-dev.yml down

echo "✅ 로컬 개발 환경이 중지되었습니다."