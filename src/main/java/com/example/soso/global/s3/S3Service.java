// src/main/java/com/example/soso/global/s3/S3Service.java
package com.example.soso.global.s3;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3; // S3Config의 @Bean으로 주입

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.s3.base-url}")
    private String baseUrl;

    @PostConstruct
    private void normalizeBaseUrl() {
        this.baseUrl = trimTrailingSlashes(baseUrl);
    }
    // 이미지 업로드
    public String uploadImage(MultipartFile file, String dir) {
        try {
            byte[] resized = ImageResizeUtil.resizeToJpg(file);
            String key = dir + "/" + UUID.randomUUID() + ".jpg";

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/jpeg")
                    // ⚠ Bucket owner enforced면 ACL 지정하지 말 것
                    .build();

            s3.putObject(put, RequestBody.fromBytes(resized));

            return buildObjectUrl(key);

        } catch (IOException e) {
            log.error("S3 이미지 업로드 실패", e);
            throw new RuntimeException("이미지 업로드 중 오류 발생", e);
        }
    }
    // 이미지 삭제
    public void deleteImage(String imageUrl) {
        if (!hasText(baseUrl) || !hasText(imageUrl) || !imageUrl.startsWith(baseUrl + "/")) {
            log.warn("유효하지 않은 이미지 URL로 삭제 요청: {}", imageUrl);
            return;
        }
        String key = extractKeyFromUrl(imageUrl);
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn("삭제 실패: 객체가 존재하지 않음 - {}", key);
            } else {
                throw e;
            }
        }
    }
    // 이미지가 존재하는지 확인
    public boolean isImageExists(String imageUrl) {
        if (!hasText(baseUrl) || !hasText(imageUrl) || !imageUrl.startsWith(baseUrl + "/")) {
            return false;
        }
        String key = extractKeyFromUrl(imageUrl);
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {           // ✅ 자식 먼저
            return false;
        } catch (S3Exception e) {                  // ✅ 부모 나중
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    // 이미지 URL에서 키 추출
    private String extractKeyFromUrl(String url) {
        if (!hasText(url) || !hasText(baseUrl)) {
            return url;
        }

        String prefix = baseUrl + "/";
        if (!url.startsWith(prefix)) {
            return url;
        }

        return url.substring(prefix.length());
    }

    private String buildObjectUrl(String key) {
        return baseUrl + "/" + key;
    }

    private String trimTrailingSlashes(String value) {
        if (value == null) {
            return null;
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
