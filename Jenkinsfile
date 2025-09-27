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
                    // 기존 컨테이너 정리
                    sh '''
                        set -eux
                        echo "=== Cleaning up existing containers ==="
                        docker stop soso-server || true
                        docker rm soso-server || true

                        echo "=== Cleaning up unused images ==="
                        docker image prune -f || true

                        echo "=== Current Docker images ==="
                        docker images | grep soso-server || echo "No soso-server images found"
                    '''

                    // 환경 변수 파일로 새 컨테이너 시작
                    withCredentials([file(credentialsId: 'soso-env', variable: 'ENV_FILE')]) {
                        sh '''
                            set -eux
                            echo "=== Starting new container with environment file ==="

                            # 환경 변수 파일을 Docker run에서 직접 사용
                            docker run -d \
                                --name soso-server \
                                --restart unless-stopped \
                                -p 8080:8080 \
                                --env-file "$ENV_FILE" \
                                "${APP_IMAGE}"

                            echo "=== Container started successfully ==="
                            docker ps | grep soso-server

                            echo "=== Container logs (first 20 lines) ==="
                            docker logs soso-server --tail 20 || true

                            echo "=== Waiting for application to start ==="
                            sleep 10

                            echo "=== Health check ==="
                            for i in {1..30}; do
                                if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "✅ Application is healthy!"
                                    break
                                elif [ $i -eq 30 ]; then
                                    echo "❌ Health check failed after 30 attempts"
                                    echo "Container logs:"
                                    docker logs soso-server --tail 50
                                    exit 1
                                else
                                    echo "Attempt $i: Application not ready yet, waiting..."
                                    sleep 2
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
                            docker logs soso-server --tail 100 || true
                            echo "=== Stopping failed container ==="
                            docker stop soso-server || true
                            docker rm soso-server || true
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
