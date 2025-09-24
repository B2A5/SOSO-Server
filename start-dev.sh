#!/bin/bash
# 로컬 개발 환경 시작 스크립트

echo "🚀 SOSO 로컬 개발 환경을 시작합니다..."

# 1. DB/Redis 컨테이너 시작
echo "📦 DB와 Redis 컨테이너를 시작합니다..."
docker compose -f compose-dev.yml up -d

# 2. 컨테이너 헬스체크 대기
echo "⏳ 컨테이너 준비를 기다립니다..."
sleep 10

# 3. 컨테이너 상태 확인
echo "🔍 컨테이너 상태를 확인합니다..."
docker compose -f compose-dev.yml ps

echo ""
echo "✅ 로컬 개발 환경 준비 완료!"
echo ""
echo "📝 다음 단계:"
echo "   1. IntelliJ/VS Code에서 .env.local 파일 로드 설정"
echo "   2. Spring Boot 애플리케이션 실행"
echo "   3. http://localhost:8080 에서 확인"
echo ""
echo "🛑 종료시: ./stop-dev.sh 실행"