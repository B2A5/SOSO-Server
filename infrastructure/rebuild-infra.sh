#!/bin/bash

# =============================================================================
# SOSO Infrastructure Rebuild Script
# 인프라 컨테이너 재빌드 시 자동으로 설정을 복구하는 스크립트
# =============================================================================

set -e  # 에러 발생 시 스크립트 중단

echo "🏗️ SOSO Infrastructure Rebuild Started"
echo "========================================"

# 현재 디렉토리 확인
if [ ! -f "compose.yml" ]; then
    echo "❌ Error: compose.yml not found. Please run this script from the project root."
    exit 1
fi

# 서비스 선택
REBUILD_JENKINS=${1:-false}
REBUILD_PROXY=${2:-false}
REBUILD_ALL=${3:-false}

if [ "$REBUILD_ALL" = "true" ]; then
    REBUILD_JENKINS=true
    REBUILD_PROXY=true
fi

echo "📋 Rebuild Configuration:"
echo "   • Jenkins: $REBUILD_JENKINS"
echo "   • Proxy: $REBUILD_PROXY"
echo ""

# 1. Jenkins 재빌드
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo "🔄 Rebuilding Jenkins..."

    # Jenkins 컨테이너 중지 및 제거
    echo "   • Stopping Jenkins container..."
    docker compose stop jenkins || true
    docker compose rm -f jenkins || true

    # Jenkins 이미지 재빌드
    echo "   • Rebuilding Jenkins image..."
    docker compose build --no-cache jenkins

    # Jenkins 컨테이너 시작
    echo "   • Starting Jenkins container..."
    docker compose up -d jenkins

    # Jenkins 건강 체크 대기
    echo "   • Waiting for Jenkins to be healthy..."
    timeout 300 bash -c '
        until docker compose ps jenkins | grep -q "healthy"; do
            echo "     - Jenkins starting..."
            sleep 10
        done
    ' || {
        echo "❌ Jenkins failed to start within 5 minutes"
        echo "📋 Jenkins logs:"
        docker compose logs jenkins --tail 20
        exit 1
    }

    echo "✅ Jenkins rebuild completed successfully!"
    echo ""
fi

# 2. Proxy 재빌드
if [ "$REBUILD_PROXY" = "true" ]; then
    echo "🔄 Rebuilding Proxy..."

    # Proxy 컨테이너 중지 및 제거
    echo "   • Stopping Proxy container..."
    docker compose stop proxy || true
    docker compose rm -f proxy || true

    # Proxy 이미지 풀 (Caddy는 빌드하지 않고 풀)
    echo "   • Pulling latest Proxy image..."
    docker compose pull proxy

    # Proxy 컨테이너 시작
    echo "   • Starting Proxy container..."
    docker compose up -d proxy

    # Proxy 건강 체크 대기
    echo "   • Waiting for Proxy to be healthy..."
    timeout 120 bash -c '
        until docker compose ps proxy | grep -q "healthy"; do
            echo "     - Proxy starting..."
            sleep 5
        done
    ' || {
        echo "❌ Proxy failed to start within 2 minutes"
        echo "📋 Proxy logs:"
        docker compose logs proxy --tail 20
        exit 1
    }

    echo "✅ Proxy rebuild completed successfully!"
    echo ""
fi

# 3. 전체 시스템 상태 확인
echo "🔍 Final System Status:"
docker compose ps

# 4. 자동 설정 확인 (Jenkins의 경우)
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo ""
    echo "🔧 Verifying Jenkins auto-configuration..."

    # Git 설정 확인
    sleep 10  # Jenkins 완전 시작 대기
    GIT_CONFIG_CHECK=$(docker compose exec -T jenkins git config --global --list | grep user || echo "")

    if [ -n "$GIT_CONFIG_CHECK" ]; then
        echo "✅ Git configuration auto-applied:"
        echo "$GIT_CONFIG_CHECK"
    else
        echo "⚠️  Git configuration not detected - may need manual setup"
    fi
fi

echo ""
echo "🎉 Infrastructure rebuild completed!"
echo "🌐 Access URLs:"
echo "   • Main Site: https://soso.dreampaste.com"
echo "   • Jenkins: https://soso.dreampaste.com/jenkins/"
echo ""

# 5. 다음 단계 안내
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo "📋 Next Steps for Jenkins:"
    echo "   1. Visit Jenkins UI and verify GitHub integration"
    echo "   2. Test a manual build to ensure everything works"
    echo "   3. Check Jenkins logs if any issues occur:"
    echo "      docker compose logs jenkins --tail 50"
    echo ""
fi

echo "✅ Script completed successfully!"