FROM jenkins/jenkins:lts-jdk21

USER root

# Docker CLI 설치
RUN apt-get update && \
    apt-get install -y docker.io curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Docker Compose V2 플러그인 설치
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep -o '"tag_name": "[^"]*' | grep -o '[^"]*$') && \
    curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 하위 호환성을 위한 docker-compose 심볼릭 링크도 생성
RUN ln -s /usr/local/lib/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# Jenkins 사용자를 docker 그룹에 추가
RUN groupadd -g 999 docker || true && \
    usermod -aG docker jenkins

# Docker 소켓 권한 설정 스크립트
COPY docker-socket-fix.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-socket-fix.sh

# root로 유지 (compose.yml의 user: root와 일치)
# 컨테이너 시작 시 소켓 권한 자동 수정
ENTRYPOINT ["/usr/local/bin/docker-socket-fix.sh"]