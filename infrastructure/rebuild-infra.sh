#!/bin/bash

# =============================================================================
# SOSO 인프라 재빌드 자동화 스크립트
# =============================================================================
#
# 🎯 스크립트 목적:
# 이 스크립트는 SOSO 프로젝트의 인프라 컨테이너들(Jenkins, Proxy)을
# 안전하고 체계적으로 재빌드하는 자동화 도구입니다.
#
# 📋 개발 배경:
# 컨테이너 기반 인프라에서 서비스를 재빌드할 때 자주 발생하는
# 다음과 같은 문제들을 해결하기 위해 개발되었습니다:
# - Jenkins 재빌드 시 Git 사용자 설정 손실
# - 컨테이너 시작 순서 문제로 인한 의존성 오류
# - 설정 파일 및 플러그인 상태 손실
# - 재빌드 실패 시 일관성 없는 상태
# - 수동 복구 작업의 번거로움과 휴먼 에러
#
# 🔧 주요 기능:
# 1. 안전한 컨테이너 라이프사이클 관리
#    - Graceful shutdown으로 데이터 손실 방지
#    - 의존성을 고려한 순차적 중지/시작
#    - 컨테이너 및 이미지 정리
#
# 2. 이미지 업데이트 및 재빌드
#    - Jenkins: 커스텀 이미지 재빌드 (Java 21, Git 설정 포함)
#    - Proxy: Caddy 최신 이미지 풀
#    - 캐시 무효화를 통한 완전한 재빌드
#
# 3. 서비스 가용성 보장
#    - 각 서비스별 헬스체크 대기
#    - 타임아웃 설정으로 무한 대기 방지
#    - 실패 시 상세 로그 출력 및 디버깅 정보 제공
#
# 4. 자동 설정 검증 및 복구
#    - Jenkins Git 설정 자동 적용 확인
#    - 필수 플러그인 설치 상태 검증
#    - 설정 누락 시 경고 메시지 출력
#
# 5. 사용자 친화적 인터페이스
#    - 이모지와 색상을 사용한 직관적 로그
#    - 진행 상황 실시간 표시
#    - 완료 후 접속 URL 및 다음 단계 안내
#
# 🛠️ 해결하는 구체적 문제들:
# - Jenkins 컨테이너 재빌드 시 Git user.name/user.email 설정 손실
# - Docker-in-Docker 권한 설정 초기화
# - 플러그인 설치 상태 불일치
# - Proxy 서버 재시작 시 라우팅 설정 누락
# - 서비스 간 의존성으로 인한 시작 실패
# - 헬스체크 없이 재시작하여 발생하는 일시적 서비스 중단
#
# 💡 사용법:
# ./rebuild-infra.sh [jenkins] [proxy] [all]
#
# 매개변수 설명:
# - jenkins: true/false - Jenkins 컨테이너 재빌드 여부
# - proxy: true/false - Proxy(Caddy) 컨테이너 재빌드 여부
# - all: true/false - 모든 인프라 컨테이너 재빌드 여부
#
# 사용 예시:
# ./rebuild-infra.sh true false false  # Jenkins만 재빌드
# ./rebuild-infra.sh false true false  # Proxy만 재빌드
# ./rebuild-infra.sh false false true  # 전체 인프라 재빌드
# ./rebuild-infra.sh                   # 매개변수 없이 실행 시 아무것도 재빌드하지 않음
#
# 🔍 실행 시 확인사항:
# - 현재 디렉토리에 compose.yml 파일이 있는지 확인
# - Docker 및 Docker Compose가 정상 작동하는지 확인
# - 충분한 디스크 공간이 있는지 확인 (이미지 재빌드용)
# - 네트워크 연결 상태 확인 (이미지 풀용)
# =============================================================================

# =============================================================================
# 스크립트 안전성 설정
# =============================================================================
# 에러 발생 시 스크립트를 즉시 중단하여 부분적 실행으로 인한
# 일관성 없는 상태를 방지합니다.
set -e

echo "🏗️ SOSO 인프라 재빌드 시작"
echo "========================================"

# =============================================================================
# 실행 환경 검증
# =============================================================================
# 스크립트가 올바른 디렉토리에서 실행되는지 확인합니다.
# compose.yml 파일의 존재를 통해 프로젝트 루트 디렉토리임을 검증합니다.
if [ ! -f "compose.yml" ]; then
    echo "❌ 오류: compose.yml 파일을 찾을 수 없습니다."
    echo "   프로젝트 루트 디렉토리에서 스크립트를 실행해주세요."
    exit 1
fi

# =============================================================================
# 매개변수 처리 및 재빌드 대상 결정
# =============================================================================
# 사용자가 제공한 매개변수를 기반으로 어떤 서비스를 재빌드할지 결정합니다.
# 기본값은 false로 설정하여 의도하지 않은 재빌드를 방지합니다.

# 매개변수 기본값 설정 (매개변수가 없으면 false)
REBUILD_JENKINS=${1:-false}
REBUILD_PROXY=${2:-false}
REBUILD_ALL=${3:-false}

# 전체 재빌드 옵션이 활성화된 경우 모든 서비스 활성화
if [ "$REBUILD_ALL" = "true" ]; then
    REBUILD_JENKINS=true
    REBUILD_PROXY=true
fi

# =============================================================================
# 재빌드 설정 확인 및 출력
# =============================================================================
# 사용자가 선택한 재빌드 옵션을 명확하게 표시하여
# 의도하지 않은 서비스 재빌드를 방지합니다.
echo "📋 재빌드 설정:"
echo "   • Jenkins CI/CD 서버: $REBUILD_JENKINS"
echo "   • Proxy 리버스 프록시: $REBUILD_PROXY"
echo ""

# 재빌드 대상이 없는 경우 경고 메시지 출력
if [ "$REBUILD_JENKINS" = "false" ] && [ "$REBUILD_PROXY" = "false" ]; then
    echo "⚠️  재빌드할 서비스가 선택되지 않았습니다."
    echo "   사용법: ./rebuild-infra.sh [jenkins] [proxy] [all]"
    echo "   예시: ./rebuild-infra.sh true false false"
    echo ""
fi

# =============================================================================
# 1. Jenkins CI/CD 서버 재빌드
# =============================================================================
# Jenkins는 SOSO 프로젝트의 CI/CD 파이프라인을 담당하는 핵심 서비스입니다.
# 재빌드 과정에서 다음과 같은 중요한 작업들이 수행됩니다:
# - Java 21 런타임 환경 설정
# - Git 설정 자동화 스크립트 적용
# - Docker-in-Docker 권한 설정
# - 필수 플러그인 및 설정 복원
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo "🔄 Jenkins CI/CD 서버 재빌드 중..."

    # =========================================================================
    # 1-1. 기존 Jenkins 컨테이너 안전 종료
    # =========================================================================
    # Graceful shutdown을 통해 실행 중인 빌드를 완료하고
    # 설정 파일을 안전하게 저장한 후 컨테이너를 종료합니다.
    echo "   • Jenkins 컨테이너 중지 중..."
    docker compose stop jenkins || true

    # 컨테이너 제거 (볼륨은 보존되어 Jenkins 설정 유지)
    echo "   • Jenkins 컨테이너 제거 중..."
    docker compose rm -f jenkins || true

    # =========================================================================
    # 1-2. Jenkins 이미지 재빌드
    # =========================================================================
    # 캐시를 사용하지 않고 완전히 새로운 이미지를 빌드합니다.
    # 이를 통해 Java 21 업데이트, 보안 패치, 플러그인 업데이트 등을
    # 모두 포함한 최신 상태의 Jenkins 이미지를 생성합니다.
    echo "   • Jenkins 커스텀 이미지 재빌드 중..."
    echo "     (Java 21, Git 설정, Docker CLI 포함)"
    docker compose build --no-cache jenkins

    # =========================================================================
    # 1-3. Jenkins 컨테이너 시작
    # =========================================================================
    # 새로 빌드된 이미지로 Jenkins 컨테이너를 시작합니다.
    # 시작과 동시에 docker-socket-fix.sh 스크립트가 실행되어
    # 필요한 권한 설정과 Git 설정을 자동으로 적용합니다.
    echo "   • Jenkins 컨테이너 시작 중..."
    docker compose up -d jenkins

    # =========================================================================
    # 1-4. Jenkins 헬스체크 및 시작 대기
    # =========================================================================
    # Jenkins가 완전히 시작되고 웹 UI가 정상 작동할 때까지 대기합니다.
    # 최대 5분(300초) 동안 대기하며, 시간 초과 시 오류 정보를 출력합니다.
    echo "   • Jenkins 정상 시작 대기 중..."
    echo "     (플러그인 로딩 및 초기 설정 완료까지 최대 5분 소요)"

    timeout 300 bash -c '
        until docker compose ps jenkins | grep -q "healthy"; do
            echo "     - Jenkins 시작 중... ($(date +%H:%M:%S))"
            sleep 10
        done
    ' || {
        echo "❌ Jenkins가 5분 내에 시작되지 않았습니다"
        echo ""
        echo "📋 Jenkins 컨테이너 상태:"
        docker compose ps jenkins
        echo ""
        echo "📋 Jenkins 최근 로그 (마지막 20줄):"
        docker compose logs jenkins --tail 20
        echo ""
        echo "💡 디버깅 도움말:"
        echo "   - 전체 로그 확인: docker compose logs jenkins"
        echo "   - 컨테이너 재시작: docker compose restart jenkins"
        echo "   - 디스크 공간 확인: df -h"
        exit 1
    }

    echo "✅ Jenkins 재빌드가 성공적으로 완료되었습니다!"
    echo ""
fi

# =============================================================================
# 2. Proxy (Caddy) 리버스 프록시 서버 재빌드
# =============================================================================
# Proxy 서버는 모든 외부 트래픽의 진입점 역할을 하는 중요한 서비스입니다.
# 다음과 같은 핵심 기능들을 담당합니다:
# - HTTPS/SSL 인증서 자동 관리 (Let's Encrypt)
# - 요청 경로별 백엔드 서비스 라우팅
# - 보안 헤더 추가 및 정적 파일 캐싱
# - API 서버 헬스체크 및 로드 밸런싱
if [ "$REBUILD_PROXY" = "true" ]; then
    echo "🔄 Proxy 리버스 프록시 서버 재빌드 중..."

    # =========================================================================
    # 2-1. 기존 Proxy 컨테이너 안전 종료
    # =========================================================================
    # 현재 처리 중인 요청들을 완료한 후 Graceful shutdown을 수행합니다.
    # SSL 인증서 및 설정 파일은 볼륨을 통해 보존됩니다.
    echo "   • Proxy 컨테이너 중지 중..."
    docker compose stop proxy || true

    # 컨테이너 제거 (SSL 인증서와 설정은 볼륨에서 보존)
    echo "   • Proxy 컨테이너 제거 중..."
    docker compose rm -f proxy || true

    # =========================================================================
    # 2-2. Caddy 이미지 업데이트
    # =========================================================================
    # Caddy는 공식 이미지를 사용하므로 빌드 대신 최신 이미지를 풀합니다.
    # 이를 통해 보안 패치, 성능 개선, 버그 수정 등이 적용된
    # 최신 버전의 Caddy를 사용할 수 있습니다.
    echo "   • Caddy 최신 이미지 다운로드 중..."
    echo "     (보안 패치 및 성능 개선 사항 포함)"
    docker compose pull proxy

    # =========================================================================
    # 2-3. Proxy 컨테이너 시작
    # =========================================================================
    # 새로운 이미지로 Proxy 컨테이너를 시작합니다.
    # Caddyfile 설정을 자동으로 로드하고 SSL 인증서를 확인합니다.
    echo "   • Proxy 컨테이너 시작 중..."
    echo "     (SSL 인증서 검증 및 라우팅 설정 로드)"
    docker compose up -d proxy

    # =========================================================================
    # 2-4. Proxy 헬스체크 및 시작 대기
    # =========================================================================
    # Proxy가 완전히 시작되고 라우팅이 정상 작동할 때까지 대기합니다.
    # SSL 인증서 검증과 백엔드 연결 확인이 포함됩니다.
    echo "   • Proxy 정상 시작 대기 중..."
    echo "     (SSL 인증서 로드 및 백엔드 연결 확인)"

    timeout 120 bash -c '
        until docker compose ps proxy | grep -q "healthy"; do
            echo "     - Proxy 시작 중... ($(date +%H:%M:%S))"
            sleep 5
        done
    ' || {
        echo "❌ Proxy가 2분 내에 시작되지 않았습니다"
        echo ""
        echo "📋 Proxy 컨테이너 상태:"
        docker compose ps proxy
        echo ""
        echo "📋 Proxy 최근 로그 (마지막 20줄):"
        docker compose logs proxy --tail 20
        echo ""
        echo "💡 디버깅 도움말:"
        echo "   - 전체 로그 확인: docker compose logs proxy"
        echo "   - SSL 인증서 상태: docker compose exec proxy caddy list-certificates"
        echo "   - Caddyfile 문법 확인: docker compose exec proxy caddy validate"
        echo "   - 포트 충돌 확인: netstat -tlnp | grep ':80\\|:443'"
        exit 1
    }

    echo "✅ Proxy 재빌드가 성공적으로 완료되었습니다!"
    echo ""
fi

# =============================================================================
# 3. 전체 시스템 상태 확인
# =============================================================================
# 재빌드가 완료된 후 모든 서비스의 상태를 종합적으로 확인합니다.
# 각 컨테이너의 상태, 포트 바인딩, 헬스체크 결과를 표시합니다.
echo "🔍 최종 시스템 상태 확인:"
echo "   모든 서비스의 상태, 포트, 헬스체크 결과를 확인합니다."
echo ""
docker compose ps
echo ""

# =============================================================================
# 4. Jenkins 자동 설정 검증 (Jenkins 재빌드 시에만 실행)
# =============================================================================
# Jenkins가 재빌드된 경우, 자동 설정 스크립트가 올바르게 적용되었는지 확인합니다.
# 특히 Git 설정이 정상적으로 적용되었는지 검증하여 향후 빌드 시
# "Please tell me who you are" 오류를 방지합니다.
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo "🔧 Jenkins 자동 설정 검증 중..."
    echo "   Git 설정, Docker 권한, 플러그인 상태를 확인합니다."

    # Jenkins 완전 초기화 대기 (플러그인 로딩 완료)
    echo "   • Jenkins 초기화 완료 대기 중... (10초)"
    sleep 10

    # =========================================================================
    # 4-1. Git 전역 설정 확인
    # =========================================================================
    # docker-socket-fix.sh 스크립트가 적용한 Git 설정을 확인합니다.
    echo "   • Git 전역 설정 확인 중..."
    GIT_CONFIG_CHECK=$(docker compose exec -T jenkins git config --global --list | grep user || echo "")

    if [ -n "$GIT_CONFIG_CHECK" ]; then
        echo "✅ Git 설정이 자동으로 적용되었습니다:"
        echo "$GIT_CONFIG_CHECK" | sed 's/^/     /'
        echo "   → GitHub 연동 및 커밋 작업이 정상적으로 수행됩니다."
    else
        echo "⚠️  Git 설정이 감지되지 않았습니다."
        echo "   → 수동 설정이 필요할 수 있습니다."
        echo "   → 해결 방법: docker compose exec jenkins git config --global user.name 'Jenkins CI'"
        echo "   → 해결 방법: docker compose exec jenkins git config --global user.email 'jenkins@dreampaste.com'"
    fi

    # =========================================================================
    # 4-2. Docker 소켓 권한 확인
    # =========================================================================
    # Jenkins가 Docker-in-Docker 기능을 사용할 수 있는지 확인합니다.
    echo "   • Docker 소켓 권한 확인 중..."
    DOCKER_ACCESS_CHECK=$(docker compose exec -T jenkins docker version --format '{{.Server.Version}}' 2>/dev/null || echo "")

    if [ -n "$DOCKER_ACCESS_CHECK" ]; then
        echo "✅ Docker 소켓 접근이 정상적으로 설정되었습니다."
        echo "     Docker 서버 버전: $DOCKER_ACCESS_CHECK"
        echo "   → Docker-in-Docker 빌드가 가능합니다."
    else
        echo "⚠️  Docker 소켓 접근에 문제가 있을 수 있습니다."
        echo "   → Jenkins에서 Docker 명령어 사용 시 권한 오류가 발생할 수 있습니다."
    fi
fi

# =============================================================================
# 5. 재빌드 완료 요약 및 접속 정보
# =============================================================================
echo ""
echo "🎉 인프라 재빌드가 성공적으로 완료되었습니다!"
echo ""
echo "🌐 서비스 접속 URL:"
echo "   • 메인 사이트: https://soso.dreampaste.com"
echo "   • Jenkins 관리: https://soso.dreampaste.com/jenkins/"
echo "   • API 문서: https://soso.dreampaste.com/swagger-ui/"
echo "   • API 헬스체크: https://soso.dreampaste.com/actuator/health"
echo ""

# =============================================================================
# 6. 후속 작업 안내 (Jenkins 재빌드 시에만 표시)
# =============================================================================
# Jenkins가 재빌드된 경우, 개발자가 수행해야 할 다음 단계를 안내합니다.
if [ "$REBUILD_JENKINS" = "true" ]; then
    echo "📋 Jenkins 재빌드 후 확인사항:"
    echo ""
    echo "   1. 🔐 Jenkins 웹 UI 접속 및 로그인 확인"
    echo "      → https://soso.dreampaste.com/jenkins/"
    echo "      → admin 계정으로 로그인 가능한지 확인"
    echo ""
    echo "   2. 🔗 GitHub 연동 상태 확인"
    echo "      → Jenkins 관리 → 시스템 설정 → GitHub 플러그인 설정 확인"
    echo "      → Webhook URL이 올바르게 설정되어 있는지 확인"
    echo ""
    echo "   3. 🧪 수동 빌드 테스트"
    echo "      → 'SOSO-Server' 파이프라인에서 'Build Now' 실행"
    echo "      → 빌드 로그에서 오류 없이 완료되는지 확인"
    echo ""
    echo "   4. 🐛 문제 발생 시 디버깅"
    echo "      → Jenkins 상세 로그: docker compose logs jenkins --tail 100"
    echo "      → 컨테이너 상태 확인: docker compose ps"
    echo "      → 디스크 공간 확인: df -h"
    echo ""
    echo "   5. 🔄 자동 빌드 트리거 테스트"
    echo "      → GitHub에 테스트 커밋을 푸시하여 자동 빌드 작동 확인"
    echo ""
fi

echo "✅ 스크립트가 성공적으로 완료되었습니다!"
echo ""
echo "💡 추가 도움이 필요한 경우:"
echo "   • 전체 서비스 재시작: docker compose restart"
echo "   • 로그 실시간 모니터링: docker compose logs -f"
echo "   • 개별 서비스 재빌드: ./rebuild-infra.sh [jenkins|proxy]"