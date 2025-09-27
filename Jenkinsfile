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
                    withCredentials([file(credentialsId: 'soso-env', variable: 'ENV_FILE')]) {
                        sh '''
                            set -eux
                            cp "$ENV_FILE" .env
                            docker compose -f compose.yml --env-file .env down || true
                            docker compose -f compose.yml --env-file .env up -d --force-recreate --remove-orphans
                            rm -f .env
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
