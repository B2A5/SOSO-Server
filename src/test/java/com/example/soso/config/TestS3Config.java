package com.example.soso.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * 테스트용 S3 설정
 * 실제 S3 연결 대신 Mock을 사용
 */
@TestConfiguration
public class TestS3Config {

    @Bean
    @Primary
    public S3Client mockS3Client() {
        S3Client mockS3Client = Mockito.mock(S3Client.class);

        // PutObject 요청에 대한 Mock 응답 설정
        PutObjectResponse mockResponse = PutObjectResponse.builder()
                .eTag("mock-etag")
                .build();

        Mockito.when(mockS3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(mockResponse);

        return mockS3Client;
    }
}