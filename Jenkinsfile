pipeline {
    agent any

    environment {
        APP_IMAGE = "localTest/soso-server:latest"
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
                    ./gradlew clean test
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
                script {
                    docker.build(APP_IMAGE, '.')
                }
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
