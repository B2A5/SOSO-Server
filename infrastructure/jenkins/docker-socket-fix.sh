#!/bin/bash

# Docker 소켓 권한 수정 (root 권한으로 실행되므로 sudo 불필요)
if [ -S /var/run/docker.sock ]; then
    chown root:root /var/run/docker.sock 2>/dev/null || true
    chmod 666 /var/run/docker.sock 2>/dev/null || true
    echo "Docker socket permissions set successfully"
fi

# Jenkins workspace 권한 수정 (배포 시 .env 파일 생성 가능하도록)
if [ -d "/var/jenkins_home/workspace" ]; then
    chmod -R 755 /var/jenkins_home/workspace 2>/dev/null || true
    echo "Jenkins workspace permissions set successfully"
fi

# 원래 Jenkins 엔트리포인트 실행
exec /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"