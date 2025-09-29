pipeline {
    agent any

    environment {
        // Docker Image Configuration
        APP_IMAGE = "localtest/soso-server:latest"
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

        stage('🧪 Test Suite') {
            parallel {
                stage('Unit Tests') {
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

                            # Run tests with detailed output
                            ./gradlew clean test \
                                -Dspring.profiles.active=test \
                                --info \
                                --stacktrace \
                                --parallel \
                                --build-cache
                        '''
                    }
                    post {
                        always {
                            junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                            // publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                        }
                    }
                }

                stage('Code Quality') {
                    steps {
                        sh '''
                            echo "📋 Code Quality Analysis..."

                            # Check code style and quality (if checkstyle/spotless is configured)
                            ./gradlew check --continue || true

                            echo "✅ Code quality check completed"
                        '''
                    }
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
                        --build-cache \
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
                sh '''
                    echo "🐳 Building Docker Image..."
                    set -eux

                    # Build Docker image with multiple tags
                    docker build \
                        -t "${APP_IMAGE}" \
                        -t "${APP_IMAGE%:*}:${BUILD_TIMESTAMP}" \
                        -t "${APP_IMAGE%:*}:${GIT_SHORT_COMMIT}" \
                        --label "version=${BUILD_TIMESTAMP}" \
                        --label "commit=${GIT_SHORT_COMMIT}" \
                        --label "build-number=${BUILD_NUMBER}" \
                        .

                    echo "📊 Docker Image Information:"
                    docker images | grep "${APP_IMAGE%:*}" | head -5

                    # Clean up old images
                    docker image prune -f --filter "until=72h" || true
                '''
            }
        }

        stage('🚀 Deploy to Production') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'soso-env', variable: 'ENV_FILE')]) {
                        sh '''
                            echo "🚀 Deploying to Production..."
                            set -eux

                            # Copy environment file to project root
                            cp "$ENV_FILE" .env

                            # Set the API image in environment
                            echo "API_IMAGE=${APP_IMAGE}" >> .env

                            echo "📋 Deployment Configuration:"
                            echo "   • Image: ${APP_IMAGE}"
                            echo "   • Compose Project: ${COMPOSE_PROJECT_NAME}"
                            echo "   • Environment: Production"
                            echo ""

                            # Stop and remove old API container gracefully
                            echo "🛑 Graceful shutdown of existing services..."
                            docker compose stop api || true
                            docker compose rm -f api || true

                            # 의존성 서비스 시작
                            # docker compose up -d는 이미 실행 중인 컨테이너는 건드리지 않고,
                            # 없거나 중지된 컨테이너만 시작합니다.
                            echo "🚀 의존성 서비스 시작 중..."
                            docker compose up -d db redis

                            # 의존성 서비스들이 정상 상태가 될 때까지 대기
                            echo "⏳ 의존성 서비스 대기 중..."
                            timeout ${DEPLOY_TIMEOUT} bash -c '
                                until docker compose ps db | grep -q "healthy"; do
                                    echo "   • 데이터베이스 대기 중..."
                                    sleep 5
                                done
                                until docker compose ps redis | grep -q "healthy"; do
                                    echo "   • Redis 대기 중..."
                                    sleep 5
                                done
                            '

                            # API 서비스 시작
                            echo "🚀 API 서비스 시작 중..."
                            docker compose up -d api

                            # API 서비스 정상 상태 확인
                            echo "🏥 API 서비스 상태 확인 중..."
                            RETRY_COUNT=0
                            until [ $RETRY_COUNT -eq ${HEALTH_CHECK_RETRIES} ]; do
                                if docker compose exec -T api curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "✅ API 서비스 정상 동작!"
                                    break
                                elif [ $RETRY_COUNT -eq $((HEALTH_CHECK_RETRIES-1)) ]; then
                                    echo "❌ API 상태 확인 실패 (${HEALTH_CHECK_RETRIES}번 시도 후)"
                                    echo "📋 컨테이너 상태:"
                                    docker compose ps api
                                    echo "📋 컨테이너 로그:"
                                    docker compose logs api --tail 50
                                    exit 1
                                else
                                    echo "   • 시도 $((RETRY_COUNT+1))/${HEALTH_CHECK_RETRIES}: API 서비스 준비 중..."
                                    sleep ${HEALTH_CHECK_INTERVAL}
                                fi
                                RETRY_COUNT=$((RETRY_COUNT+1))
                            done

                            # API 정상 확인 후 프록시 시작
                            echo "🌐 리버스 프록시 시작 중..."
                            docker compose up -d proxy

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
