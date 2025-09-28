# 🔄 SOSO Server Migration Guide

기존 구버전 시스템에서 현대화된 인프라로 마이그레이션하는 가이드

## 📋 목차

- [마이그레이션 개요](#마이그레이션-개요)
- [사전 준비](#사전-준비)
- [단계별 마이그레이션](#단계별-마이그레이션)
- [구버전 정리](#구버전-정리)
- [검증 및 테스트](#검증-및-테스트)
- [롤백 계획](#롤백-계획)

## 🎯 마이그레이션 개요

### 현재 상황 (Before)
```
3개의 분리된 배포 시스템:
├── /opt/soso-api/           # 구버전 미사용 시스템
├── /srv/soso/stack/         # 현재 부분 작동 시스템
└── Jenkins 수동 배포        # 포트 충돌 발생
```

### 목표 상황 (After)
```
통합된 현대화 시스템:
├── compose.yml              # 모든 서비스 통합 관리
├── infrastructure/          # Git 기반 인프라 관리
├── Jenkinsfile             # Docker Compose 기반 CI/CD
└── 자동화된 배포 및 모니터링
```

## 🔧 사전 준비

### 1. 백업 생성

```bash
# 데이터베이스 백업
ssh dreampaste
mkdir -p ~/backups/$(date +%Y%m%d)
docker exec soso-mysql mysqldump --all-databases -u root -p > ~/backups/$(date +%Y%m%d)/mysql_backup.sql

# Redis 백업
docker exec soso-redis redis-cli --rdb - > ~/backups/$(date +%Y%m%d)/redis_backup.rdb

# 설정 파일 백업
cp -r /srv/soso ~/backups/$(date +%Y%m%d)/srv_soso_backup
cp -r /opt/soso-api ~/backups/$(date +%Y%m%d)/opt_soso_backup 2>/dev/null || true
```

### 2. Git 레포지토리 업데이트

```bash
# 로컬에서 최신 현대화 파일들을 서버에 전송
cd /Users/hwigeon/project/SOSO-Server
git add .
git commit -m "feat: 현대화된 인프라 구조 추가"
git push origin main
```

## 🚀 단계별 마이그레이션

### Phase 1: 서버에 새로운 인프라 배포

```bash
# 1. 서버에 SSH 접속
ssh dreampaste

# 2. 새로운 워킹 디렉토리로 이동 (또는 생성)
cd /srv/soso-new || mkdir -p /srv/soso-new
cd /srv/soso-new

# 3. 최신 코드 클론
git clone https://github.com/B2A5/SOSO-Server.git .

# 4. 환경변수 설정 (기존 설정을 복사)
cp /srv/soso/stack/.env .env
# 필요시 .env 파일 수정

# 5. 권한 설정
chmod +x infrastructure/scripts/*.sh
```

### Phase 2: 안전한 마이그레이션 (기존 서비스 보존)

```bash
# 1. 기존 서비스는 일단 그대로 두고 새 서비스 시작
cd /srv/soso/stack
# 기존 Jenkins(8080)와 새 Jenkins(8081)가 공존

# 2. 기존 데이터 볼륨 확인 (변경 없음)
docker volume ls | grep stack_
# stack_mysql_data (보존됨)
# stack_redis_data (있다면 보존됨)

# 3. 새로운 현대화 인프라 시작
cd /srv/soso  # Git 레포지토리 루트
docker compose up -d db redis  # 기존 데이터 사용
docker compose up -d api       # 새로운 API 서비스 (내부 8080)
docker compose up -d jenkins-new  # 새로운 Jenkins (외부 8081)
```

### Phase 3: 새로운 시스템 배포

```bash
# 1. 인프라 검증
docker compose config

# 2. 데이터베이스 및 Redis 먼저 시작
docker compose up -d db redis

# 3. 데이터 복원 (필요한 경우)
# docker run --rm -v mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql_data.tar.gz -C /data

# 4. 서비스들이 healthy 상태가 될 때까지 대기
docker compose ps

# 5. API 서비스 시작
docker compose up -d api

# 6. 헬스체크 확인
docker compose exec api curl -f http://localhost:8080/actuator/health

# 7. 프록시 시작
docker compose up -d proxy

# 8. 전체 시스템 확인
docker compose ps
```

### Phase 4: Jenkins 설정 업데이트

```bash
# Jenkins에서 새로운 Jenkinsfile이 자동으로 적용됩니다
# GitHub webhook을 통해 다음 배포부터는 자동화됩니다

# 수동으로 빌드 트리거 (테스트용)
# Jenkins 웹 UI에서 "Build Now" 클릭
```

## 🗑️ 구버전 정리

### 안전한 정리 절차

```bash
# 1. 새 시스템이 정상 작동하는지 24시간 모니터링 후 진행

# 2. 구버전 컨테이너 정리
cd /srv/soso/stack
docker compose down

# 3. 구버전 디렉토리 정리 (백업 후)
sudo mv /opt/soso-api /opt/soso-api.backup.$(date +%Y%m%d)
sudo mv /srv/soso /srv/soso.backup.$(date +%Y%m%d)

# 4. 새 디렉토리를 메인으로 이동
sudo mv /srv/soso-new /srv/soso

# 5. 심볼릭 링크 생성 (선택사항)
ln -sf /srv/soso /opt/soso-current

# 6. 불필요한 Docker 리소스 정리
docker system prune -f
docker volume prune -f
```

### 완전 정리 (1주일 후)

```bash
# 백업 파일들이 더 이상 필요 없다고 확신되면
sudo rm -rf /opt/soso-api.backup.*
sudo rm -rf /srv/soso.backup.*
```

## ✅ 검증 및 테스트

### 1. 기능 테스트

```bash
# API 엔드포인트 테스트
curl -f https://soso.dreampaste.com/actuator/health
curl -f https://soso.dreampaste.com/swagger-ui/

# Jenkins 접근 테스트
curl -f https://soso.dreampaste.com/jenkins/

# 데이터베이스 연결 테스트
docker compose exec db mysql -u soso_user -p -e "SELECT 1"
```

### 2. 성능 테스트

```bash
# 리소스 사용량 모니터링
./infrastructure/scripts/manage.sh monitor 300

# 로드 테스트 (선택사항)
# ab -n 100 -c 10 https://soso.dreampaste.com/api/health
```

### 3. CI/CD 테스트

```bash
# 코드 변경 후 자동 배포 테스트
echo "# Test change" >> README.md
git add README.md
git commit -m "test: CI/CD 파이프라인 테스트"
git push origin main

# Jenkins에서 자동 빌드 확인
```

## 🔄 롤백 계획

### 긴급 롤백 (문제 발생 시)

```bash
# 1. 새 시스템 중단
cd /srv/soso
docker compose down

# 2. 기존 시스템 복원
cd /srv/soso.backup.$(date +%Y%m%d)
docker compose up -d

# 3. 데이터 복원 (필요시)
# 백업에서 데이터베이스 복원
```

### 정상 롤백 절차

```bash
# 1. 점진적 롤백
# API만 먼저 기존 버전으로 복원
docker compose stop api
docker tag old-image-backup:latest localtest/soso-server:latest
docker compose up -d api

# 2. 전체 롤백 (필요시)
# 위의 긴급 롤백 절차 참조
```

## 📊 마이그레이션 체크리스트

### Pre-Migration
- [ ] 전체 시스템 백업 완료
- [ ] 새 인프라 코드 검증 완료
- [ ] 환경변수 설정 확인
- [ ] 다운타임 공지

### During Migration
- [ ] 기존 서비스 graceful shutdown
- [ ] 데이터 마이그레이션 완료
- [ ] 새 시스템 배포 완료
- [ ] 헬스체크 통과

### Post-Migration
- [ ] 기능 테스트 완료
- [ ] 성능 테스트 완료
- [ ] CI/CD 파이프라인 테스트 완료
- [ ] 24시간 모니터링 완료
- [ ] 구버전 정리 완료

## 🆘 문제 해결

### 일반적인 문제들

#### 1. 포트 충돌
```bash
# 기존 Jenkins가 8080 포트를 사용 중일 때
docker compose stop jenkins
# 새 compose.yml에서 Jenkins는 동일한 8080 사용하므로 문제없음
```

#### 2. 데이터베이스 연결 실패
```bash
# 네트워크 확인
docker network ls | grep soso
docker compose exec api ping db
```

#### 3. 프록시 설정 오류
```bash
# Caddyfile 검증
docker compose exec proxy caddy validate --config /etc/caddy/Caddyfile
```

## 📞 지원

마이그레이션 중 문제가 발생하면:

1. **로그 확인**: `./infrastructure/scripts/manage.sh logs`
2. **상태 확인**: `./infrastructure/scripts/manage.sh status`
3. **백업으로 롤백**: 위의 롤백 절차 참조

---

**중요**: 마이그레이션은 점진적으로 진행하고, 각 단계마다 충분한 검증을 거친 후 다음 단계로 진행하세요.