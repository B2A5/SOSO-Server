#!/bin/bash

echo "🚀 Starting Jenkins with auto-configuration..."

# Docker 소켓 권한 수정 (root 권한으로 실행되므로 sudo 불필요)
if [ -S /var/run/docker.sock ]; then
    chown root:root /var/run/docker.sock 2>/dev/null || true
    chmod 666 /var/run/docker.sock 2>/dev/null || true
    echo "✅ Docker socket permissions set successfully"
fi

# Jenkins workspace 권한 수정 (배포 시 .env 파일 생성 가능하도록)
if [ -d "/var/jenkins_home/workspace" ]; then
    chmod -R 755 /var/jenkins_home/workspace 2>/dev/null || true
    echo "✅ Jenkins workspace permissions set successfully"
fi

# Git 전역 설정 자동화 (Jenkins 컨테이너 재시작 시 자동 적용)
echo "🔧 Configuring Git settings..."
git config --global user.name "Jenkins CI" 2>/dev/null || true
git config --global user.email "jenkins@dreampaste.com" 2>/dev/null || true
git config --global init.defaultBranch "main" 2>/dev/null || true
git config --global safe.directory "*" 2>/dev/null || true
echo "✅ Git configuration completed"

# Jenkins 초기 설정이 완료될 때까지 대기 후 추가 설정 적용
(
    # 백그라운드에서 Jenkins 완전 시작 대기
    echo "⏳ Waiting for Jenkins to fully start..."
    sleep 60

    # Jenkins CLI를 통한 추가 자동 설정 (선택사항)
    echo "🔧 Applying additional Jenkins configurations..."

    # GitHub 플러그인 자동 설치 확인 (이미 설치되어 있으면 스킵)
    if [ -f /var/jenkins_home/plugins/github.hpi ]; then
        echo "✅ GitHub plugin already installed"
    else
        echo "📦 GitHub plugin not found - manual installation may be needed"
    fi

    echo "✅ Jenkins auto-configuration completed"
) &

echo "🚀 Starting Jenkins server..."
# 원래 Jenkins 엔트리포인트 실행
exec /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"