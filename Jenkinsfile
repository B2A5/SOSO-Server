pipeline {
    agent any

    environment {
        // Docker Image Configuration
        APP_IMAGE = "soso/api:latest"
        COMPOSE_PROJECT_NAME = "soso-server"

        // Deployment Configuration
        DEPLOY_TIMEOUT = "300"
        HEALTH_CHECK_RETRIES = "30"
        HEALTH_CHECK_INTERVAL = "10"
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '30', daysToKeepStr: '7'))
        skipDefaultCheckout(false)
        ansiColor('xterm')
    }

    triggers {
        githubPush()
    }

    stages {
        stage('🏗️ Prepare') {
            steps {
                script {
                    // Clean workspace and checkout
                    cleanWs()
                    checkout scm

                    // Display build information
                    sh '''
                        echo "🚀 SOSO Server CI/CD Pipeline Started"
                        echo "📋 Build Information:"
                        echo "   • Branch: ${GIT_BRANCH}"
                        echo "   • Commit: ${GIT_COMMIT}"
                        echo "   • Build: ${BUILD_NUMBER}"
                        echo "   • Date: $(date '+%Y-%m-%d %H:%M:%S %Z')"
                        echo "   • Image: ${APP_IMAGE}"
                        echo ""
                    '''

                    // Set dynamic variables
                    env.BUILD_TIMESTAMP = sh(script: "date +%Y%m%d-%H%M%S", returnStdout: true).trim()
                    env.GIT_SHORT_COMMIT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                }
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                sh '''
                    echo "🧪 Running Unit Tests..."
                    set -eux

                    # Test Environment Configuration
                    export SPRING_PROFILES_ACTIVE=test
                    export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                    export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.h2.Driver"
                    export SPRING_DATASOURCE_USERNAME="sa"
                    export SPRING_DATASOURCE_PASSWORD=""
                    export SPRING_JPA_HIBERNATE_DDL_AUTO="create-drop"
                    export SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.H2Dialect"
                    export SPRING_SESSION_STORE_TYPE="none"

                    echo "📊 Test Configuration:"
                    echo "   • Profile: $SPRING_PROFILES_ACTIVE"
                    echo "   • Database: H2 In-Memory"
                    echo "   • Java: $(java -version 2>&1 | head -1)"
                    echo ""

                    # 테스트 결과 파일만 삭제 (컴파일 캐시는 유지)
                    echo "🧹 Cleaning test output files..."
                    rm -rf build/test-results || true
                    rm -rf build/reports/tests || true

                    # 손상된 Gradle 캐시 파일만 정리
                    rm -rf .gradle/8.*/fileHashes/fileHashes.lock || true
                    rm -rf .gradle/buildOutputCleanup/buildOutputCleanup.lock || true

                    # Gradle 데몬 재시작 (깨끗한 상태 보장)
                    echo "🔄 Restarting Gradle daemon for clean state..."
                    ./gradlew --stop || true

                    # 최적화된 테스트 실행
                    echo "🚀 Running tests with optimizations..."
                    ./gradlew test \
                        -Dspring.profiles.active=test \
                        --parallel \
                        --max-workers=4 \
                        --build-cache \
                        --info \
                        --stacktrace

                    echo "✅ Tests completed successfully"
                '''
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                    // publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('🏗️ Build Application') {
            steps {
                sh '''
                    echo "🏗️ Building Application JAR..."
                    set -eux

                    # Build the application
                    ./gradlew bootJar \
                        --info \
                        --parallel

                    # Display build results
                    echo "📦 Build Results:"
                    ls -la build/libs/

                    # Extract version information
                    JAR_FILE=$(find build/libs -name "*.jar" -not -name "*plain*" | head -1)
                    if [ -f "$JAR_FILE" ]; then
                        JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
                        echo "   • JAR File: $(basename "$JAR_FILE")"
                        echo "   • Size: $JAR_SIZE"
                    fi
                '''

                archiveArtifacts artifacts: 'build/libs/*.jar',
                               allowEmptyArchive: false,
                               onlyIfSuccessful: true
            }
        }

        stage('🐳 Build Docker Image') {
            steps {
                script {
                    echo "🔍 1. compose.yml 변경 여부 체크"
                    //HEAD 커밋과 그 전 커밋의 compose.yml 파일 변경 여부 확인
                    def changedFiles = sh(
                        script: "git diff --name-only HEAD~1 HEAD || true",
                        returnStdout: true
                    ).trim()
                    echo "Changed files: ${changedFiles}"
                    echo "📂 Changed files (HEAD~1..HEAD):"
                    echo changedFiles ?: "   (no changes detected)"

                    // compose.yml이 변경됐는지 여부
                    def composeChanged = false
                    if (changedFiles) {
                        composeChanged = changedFiles
                            .readLines()
                            .any { it == 'compose.yml' || it.endsWith('/compose.yml') }
                    }

                    if (composeChanged) {
                        echo "🔄 compose.yml changed → using DOCKER BUILD with --no-cache"
                        env.NO_CACHE_OPTION = "--no-cache"
                    } else {
                        echo "✨ compose.yml not changed → using normal docker build cache"
                        env.NO_CACHE_OPTION = ""
                    }
                    def imageBase = APP_IMAGE.split(':')[0] // "localtest/soso-server"
                
                    sh """
                        echo "🐳 Building Docker Image with cache option..."
                        set -eux

                        echo "   • NO_CACHE_OPTION: '${env.NO_CACHE_OPTION}'"

                        # Build Docker image with multiple tags
                        docker build ${env.NO_CACHE_OPTION} \
                            -t "${APP_IMAGE}" \
                            -t "${imageBase}:${BUILD_TIMESTAMP}" \
                            -t "${imageBase}:${GIT_SHORT_COMMIT}" \
                            --label "version=${BUILD_TIMESTAMP}" \
                            --label "commit=${GIT_SHORT_COMMIT}" \
                            --label "build-number=${BUILD_NUMBER}" \
                            .

                        echo "📊 Docker Image Information:"
                        docker images | grep "${imageBase}" | head -5

                        # Clean up old images
                        docker image prune -f --filter "until=72h" || true
                    """
                }
            }
        }

        stage('🚀 Deploy to Production') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'soso-env', variable: 'ENV_FILE')]) {
                        sh '''
                            echo "🚀 Deploying to Production..."
                            set -eux

                            # =============================================================
                            # 영구 배포 디렉토리 사용 (Jenkins workspace와 독립적)
                            # =============================================================
                            #
                            # 왜 이렇게 하는가?
                            # 1. Jenkins workspace는 cleanWs()로 삭제됨
                            # 2. Docker 볼륨 마운트가 workspace 경로를 참조하면 안 됨
                            # 3. 서버의 영구 디렉토리를 배포 기준점으로 사용
                            # 4. 레포 중심의 인프라 배포 (Infrastructure as Code)
                            #
                            # 장점:
                            # - Caddyfile 등 설정 파일 경로가 항상 유효
                            # - 컨테이너 재시작 시에도 마운트 경로 보존
                            # - Git 기반 버전 관리 및 롤백 가능
                            # =============================================================
                            DEPLOY_DIR=/srv/soso/app/SOSO-Server

                            echo "📂 배포 디렉토리: $DEPLOY_DIR"

                            # 배포 디렉토리가 없으면 생성
                            if [ ! -d "$DEPLOY_DIR" ]; then
                                echo "⚠️  배포 디렉토리가 없습니다. 생성 중..."
                                mkdir -p "$DEPLOY_DIR"
                                cd "$DEPLOY_DIR"
                                git clone https://github.com/B2A5/SOSO-Server.git .
                            fi

                            # 배포 디렉토리로 이동
                            cd "$DEPLOY_DIR"

                            # 최신 코드로 업데이트 (현재 브랜치 기준)
                            echo "🔄 최신 코드로 업데이트 중..."
                            git fetch origin
                            git reset --hard origin/${GIT_COMMIT}

                            echo "✅ 현재 커밋:"
                            git log -1 --oneline
                            echo ""

                            # Copy environment file to deployment directory
                            cp "$ENV_FILE" "$DEPLOY_DIR/.env"

                            # Set the API image in environment
                            echo "API_IMAGE=${APP_IMAGE}" >> "$DEPLOY_DIR/.env"

                            echo "📋 Deployment Configuration:"
                            echo "   • Deploy Dir: $DEPLOY_DIR"
                            echo "   • Image: ${APP_IMAGE}"
                            echo "   • Branch: ${GIT_BRANCH##*/}"
                            echo "   • Compose Project: ${COMPOSE_PROJECT_NAME}"
                            echo "   • Environment: Production"
                            echo ""

                            # =============================================================
                            # 무중단 배포 (Zero Downtime Deployment)
                            # =============================================================
                            #
                            # 전략:
                            # 1. 의존성 서비스(DB, Redis)는 이미 실행 중이면 스킵
                            # 2. API는 롤링 업데이트 (새 컨테이너 시작 → 헬스체크 → 이전 컨테이너 제거)
                            # 3. Proxy는 절대 재시작하지 않음 (영구 유지)
                            #
                            # 다운타임: 0초
                            # =============================================================

                            # 의존성 서비스 확인 및 시작
                            echo "🔍 의존성 서비스 확인 중..."
                            docker compose up -d --no-deps db redis

                            # 의존성 서비스들이 정상 상태가 될 때까지 대기
                            echo "⏳ 의존성 서비스 헬스체크..."
                            timeout ${DEPLOY_TIMEOUT} bash -c '
                                until docker compose ps db | grep -q "healthy"; do
                                    echo "   • 데이터베이스 준비 중..."
                                    sleep 5
                                done
                                until docker compose ps redis | grep -q "healthy"; do
                                    echo "   • Redis 준비 중..."
                                    sleep 5
                                done
                            '
                            echo "✅ 의존성 서비스 정상"

                            # =============================================================
                            # API 무중단 배포 (Rolling Update)
                            # =============================================================
                            #
                            # docker compose up -d --no-deps api 동작:
                            # 1. 새 API 컨테이너 생성 (이전 컨테이너는 계속 실행)
                            # 2. 새 컨테이너 시작 및 헬스체크 대기
                            # 3. 헬스체크 통과 시 네트워크에 추가
                            # 4. Proxy가 자동으로 새 컨테이너로 라우팅 시작
                            # 5. 이전 컨테이너 graceful shutdown 및 제거
                            #
                            # 사용자 입장: 서비스 중단 없음 ✅
                            # =============================================================
                            echo "🚀 API 무중단 배포 시작..."
                            echo "   • 현재 실행 중인 API: $(docker compose ps api --format '{{.Status}}' 2>/dev/null || echo '없음')"

                            # --no-deps: 의존성 서비스는 건드리지 않음
                            # --wait: 헬스체크 통과까지 대기
                            # --wait-timeout: 최대 대기 시간
                            docker compose up -d --no-deps --wait --wait-timeout 180 api

                            # 최종 헬스체크 확인 (추가 안전장치)
                            echo "🏥 API 최종 헬스체크..."
                            RETRY_COUNT=0
                            until [ $RETRY_COUNT -eq 10 ]; do
                                if docker compose ps api | grep -q "healthy"; then
                                    echo "✅ API 무중단 배포 완료!"
                                    echo "   • 새 API 컨테이너: $(docker compose ps api --format '{{.ID}}' | head -1)"
                                    break
                                elif [ $RETRY_COUNT -eq 9 ]; then
                                    echo "❌ API 헬스체크 실패"
                                    echo "📋 컨테이너 상태:"
                                    docker compose ps api
                                    echo "📋 최근 로그:"
                                    docker compose logs api --tail 50
                                    exit 1
                                else
                                    echo "   • 헬스체크 대기 중... ($((RETRY_COUNT+1))/10)"
                                    sleep 5
                                fi
                                RETRY_COUNT=$((RETRY_COUNT+1))
                            done

                            # =============================================================
                            # Proxy 확인 및 시작 (최초 1회만 또는 필요 시)
                            # =============================================================
                            #
                            # Proxy는 영구 유지됩니다:
                            # - 이미 실행 중이면 그대로 유지
                            # - 없으면 시작
                            # - 충돌 시 기존 컨테이너 제거 후 시작
                            # =============================================================
                            echo "🌐 Proxy 배포 중..."

                            # Proxy가 없으면 생성, 있으면 설정 리로드
                            if docker compose ps proxy | grep -q "proxy"; then
                                echo "🔄 Proxy 설정 리로드 중 (무중단)..."

                                # Caddy 설정 무중단 리로드
                                docker exec soso-proxy caddy reload --config /etc/caddy/Caddyfile --force 2>&1 || {
                                    echo "⚠️  리로드 실패, Proxy 재시작 중..."
                                    docker compose restart proxy
                                    sleep 3
                                }

                                echo "✅ Proxy 설정 업데이트 완료"
                            else
                                echo "🔄 Proxy 시작 중..."

                                # 혹시 모를 충돌 방지 (수동 실행 컨테이너 등)
                                docker rm -f soso-proxy 2>/dev/null || true

                                # Proxy 시작 (의존성: API healthy)
                                docker compose up -d --no-deps proxy

                                # Proxy 헬스체크
                                sleep 3
                                if docker compose ps proxy | grep -q "healthy"; then
                                    echo "✅ Proxy 정상 시작 완료"
                                else
                                    echo "⚠️  Proxy 헬스체크 대기 중..."
                                fi
                            fi

                            # 최종 시스템 상태 확인
                            echo "🔍 최종 시스템 상태 확인..."
                            docker compose ps

                            echo "✅ 배포 완료!"
                            echo ""
                            echo "🌐 Service URLs:"
                            echo "   • Main Site: https://soso.dreampaste.com"
                            echo "   • API Docs: https://soso.dreampaste.com/swagger-ui/"
                            echo "   • Jenkins: https://soso.dreampaste.com/jenkins/"
                            echo ""
                        '''
                    }
                }
            }
            post {
                failure {
                    script {
                        sh '''
                            echo "❌ Deployment failed - Rolling back..."

                            # 배포 디렉토리로 이동
                            DEPLOY_DIR=/srv/soso/app/SOSO-Server
                            cd "$DEPLOY_DIR"

                            # Show current status
                            echo "📋 Current Status:"
                            docker compose ps || true

                            # Show logs for debugging
                            echo "📋 Service Logs:"
                            docker compose logs api --tail 100 || true

                            # Stop failed services
                            echo "🛑 Stopping failed services..."
                            docker compose stop api || true
                            docker compose rm -f api || true

                            echo "🔄 Rollback completed"
                        '''
                    }
                }
                success {
                    script {
                        sh '''
                            echo "🎉 Deployment Success!"

                            # 배포 디렉토리로 이동
                            DEPLOY_DIR=/srv/soso/app/SOSO-Server
                            cd "$DEPLOY_DIR"

                            echo "📊 Final Status:"
                            docker compose ps
                            echo ""
                            echo "💾 Cleaning up old images..."
                            docker image prune -f --filter "until=24h" || true
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                sh '''
                    echo "🧹 Pipeline Cleanup..."
                    # Clean up temporary files
                    rm -f .env || true
                '''
                cleanWs()
            }
        }
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        unstable {
            echo '⚠️ Pipeline completed with warnings'
        }
    }
}
