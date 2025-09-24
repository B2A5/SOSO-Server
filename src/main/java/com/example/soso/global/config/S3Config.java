package com.example.soso.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 설정 클래스
 */
@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.region:ap-northeast-2}")
    private String region;

    /**
     * S3 클라이언트 Bean 생성
     *
     * AWS 자격 증명은 다음 순서로 자동 탐지됩니다:
     * 1. 환경 변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * 2. Java 시스템 속성
     * 3. ~/.aws/credentials 파일
     * 4. IAM 역할 (EC2에서 실행 시)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}