package com.example.soso.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * AWS S3 설정 클래스
 */
@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.region:ap-northeast-2}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${spring.cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Value("${spring.cloud.aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

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
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (hasText(accessKey) && hasText(secretKey)) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (pathStyleAccess) {
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
