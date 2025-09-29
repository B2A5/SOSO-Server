FROM jenkins/jenkins:lts-jdk17

USER root

# Docker CLI 설치
RUN apt-get update && \
    apt-get install -y docker.io && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Jenkins 사용자를 docker 그룹에 추가 (선택사항, root로 실행하지만 유지)
RUN groupadd -g 999 docker || true && \
    usermod -aG docker jenkins

# Docker 소켓 권한 설정 스크립트
COPY docker-socket-fix.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-socket-fix.sh

# root로 유지 (compose.yml의 user: root와 일치)
# 컨테이너 시작 시 소켓 권한 자동 수정
ENTRYPOINT ["/usr/local/bin/docker-socket-fix.sh"]