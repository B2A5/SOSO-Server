pipeline {
    agent any

    environment {
        APP_IMAGE = "localtest/soso-server:latest"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Gradle Test') {
            steps {
                sh '''
                    set -eux
                    export SPRING_PROFILES_ACTIVE=test
                    export SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                    export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.h2.Driver"
                    export SPRING_DATASOURCE_USERNAME="sa"
                    export SPRING_DATASOURCE_PASSWORD=""
                    export SPRING_JPA_HIBERNATE_DDL_AUTO="create-drop"
                    export SPRING_JPA_DATABASE_PLATFORM="org.hibernate.dialect.H2Dialect"
                    export SPRING_SESSION_STORE_TYPE="none"
                    echo "Running tests with profile: $SPRING_PROFILES_ACTIVE"
                    echo "Using database: $SPRING_DATASOURCE_URL"
                    echo "Java version: $(java -version 2>&1 | head -1)"
                    ./gradlew clean test -Dspring.profiles.active=test --info --stacktrace
                '''
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Jar') {
            steps {
                sh '''
                    set -eux
                    ./gradlew bootJar
                '''
                archiveArtifacts artifacts: 'build/libs/*.jar', onlyIfSuccessful: true
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eux
                    echo "Building Docker image: ${APP_IMAGE}"
                    docker build -t "${APP_IMAGE}" .
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // 환경 변수 파일로 새 컨테이너 시작
                    withCredentials([file(credentialsId: 'soso-env', variable: 'ENV_FILE')]) {
                        sh '''
                            set -eux
                            echo "=== Cleaning up existing containers ==="
                            docker stop soso-api || true
                            docker rm soso-api || true

                            echo "=== Cleaning up unused images ==="
                            docker image prune -f || true

                            echo "=== Starting new container with environment file ==="
                            # 기존 컨테이너들과 같은 네트워크에 연결
                            NETWORK_NAME=$(docker ps --format "table {{.Names}}\t{{.Networks}}" | grep soso-mysql | awk '{print $2}' | head -1)

                            if [ -n "$NETWORK_NAME" ] && [ "$NETWORK_NAME" != "bridge" ]; then
                                echo "Connecting to existing network: $NETWORK_NAME"
                                NETWORK_OPTION="--network $NETWORK_NAME"
                            else
                                echo "Using default bridge network"
                                NETWORK_OPTION=""
                            fi

                            # 환경 변수 파일을 Docker run에서 직접 사용
                            # 기존 soso-api 컨테이너와 호환성을 위해 동일한 이름 사용
                            docker run -d \
                                --name soso-api \
                                --restart unless-stopped \
                                $NETWORK_OPTION \
                                --env-file "$ENV_FILE" \
                                "${APP_IMAGE}"

                            echo "=== Container started successfully ==="
                            docker ps | grep soso-api

                            echo "=== Waiting for application to start ==="
                            sleep 15

                            echo "=== Container logs (first 30 lines) ==="
                            docker logs soso-api --tail 30 || true

                            echo "=== Health check ==="
                            for i in {1..20}; do
                                # soso-api 컨테이너 내부 포트 8080으로 헬스체크
                                if docker exec soso-api curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "✅ Application is healthy (actuator)!"
                                    break
                                elif docker exec soso-api curl -f http://localhost:8080/ > /dev/null 2>&1; then
                                    echo "✅ Application is responding (root)!"
                                    break
                                elif [ $i -eq 20 ]; then
                                    echo "❌ Health check failed after 20 attempts"
                                    echo "Container status:"
                                    docker ps | grep soso-api || echo "Container not running"
                                    echo "Container logs:"
                                    docker logs soso-api --tail 50
                                    echo "Container internal health:"
                                    docker exec soso-api ss -tlnp | grep 8080 || echo "Port 8080 not listening inside container"
                                    exit 1
                                else
                                    echo "Attempt $i: Application not ready yet, waiting..."
                                    sleep 3
                                fi
                            done
                        '''
                    }
                }
            }
            post {
                failure {
                    script {
                        sh '''
                            echo "=== Deployment failed, showing container logs ==="
                            docker logs soso-api --tail 100 || true
                            echo "=== Stopping failed container ==="
                            docker stop soso-api || true
                            docker rm soso-api || true
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
