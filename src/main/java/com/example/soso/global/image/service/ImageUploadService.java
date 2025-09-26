package com.example.soso.global.image.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * S3 기반 이미지 업로드 서비스
 *
 * 주요 기능:
 * - S3에 이미지 업로드 (최대 4장)
 * - 파일 형식 및 크기 검증
 * - UUID 기반 고유 파일명 생성
 * - 병렬 업로드 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.base-url}")
    private String baseUrl;

    // 지원하는 이미지 형식
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 최대 이미지 개수
    private static final int MAX_IMAGE_COUNT = 4;

    // 병렬 업로드를 위한 스레드 풀
    private final Executor uploadExecutor = Executors.newFixedThreadPool(4);

    /**
     * 여러 이미지를 S3에 업로드합니다.
     *
     * @param images 업로드할 이미지 파일들
     * @param directory S3 내 저장할 디렉토리 (예: "freeboard")
     * @return 업로드된 이미지들의 URL 목록
     * @throws IllegalArgumentException 파일 검증 실패 시
     * @throws RuntimeException 업로드 실패 시
     */
    public List<String> uploadImages(List<MultipartFile> images, String directory) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        // 이미지 개수 검증
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGE_COUNT + "장까지 업로드 가능합니다.");
        }

        log.info("이미지 업로드 시작: count={}, directory={}", images.size(), directory);

        // 각 파일 검증
        validateImages(images);

        // 병렬 업로드 실행
        List<CompletableFuture<String>> uploadFutures = new ArrayList<>();

        for (MultipartFile image : images) {
            CompletableFuture<String> future = CompletableFuture
                    .supplyAsync(() -> uploadSingleImage(image, directory), uploadExecutor);
            uploadFutures.add(future);
        }

        // 모든 업로드 완료 대기 및 결과 수집
        List<String> uploadedUrls = uploadFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        log.info("이미지 업로드 완료: count={}, urls={}", uploadedUrls.size(), uploadedUrls);
        return uploadedUrls;
    }

    /**
     * 단일 이미지를 S3에 업로드합니다.
     *
     * @param image 업로드할 이미지 파일
     * @param directory S3 내 저장할 디렉토리
     * @return 업로드된 이미지의 URL
     */
    public String uploadSingleImage(MultipartFile image, String directory) {
        validateSingleImage(image);

        String fileName = generateUniqueFileName(image.getOriginalFilename(), directory);
        String key = directory + "/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(image.getContentType())
                    .contentLength(image.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));

            String imageUrl = buildObjectUrl(key);
            log.debug("이미지 업로드 성공: key={}, url={}", key, imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("이미지 업로드 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 이미지를 삭제합니다.
     *
     * @param imageUrl 삭제할 이미지 URL
     */
    public void deleteImage(String imageUrl) {
        if (!hasText(baseUrl) || !hasText(imageUrl) || !imageUrl.startsWith(baseUrl + "/")) {
            log.warn("유효하지 않은 이미지 URL로 삭제 요청: {}", imageUrl);
            return;
        }

        String key = imageUrl.substring((baseUrl + "/").length());

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("이미지 삭제 완료: key={}", key);

        } catch (Exception e) {
            log.error("이미지 삭제 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("이미지 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여러 이미지를 S3에서 삭제합니다.
     *
     * @param imageUrls 삭제할 이미지 URL 목록
     */
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        log.info("이미지 일괄 삭제 시작: count={}", imageUrls.size());

        List<CompletableFuture<Void>> deleteFutures = imageUrls.stream()
                .map(url -> CompletableFuture.runAsync(() -> deleteImage(url), uploadExecutor))
                .toList();

        // 모든 삭제 작업 완료 대기
        CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();

        log.info("이미지 일괄 삭제 완료: count={}", imageUrls.size());
    }

    /**
     * 이미지 파일들의 유효성을 검증합니다.
     */
    private void validateImages(List<MultipartFile> images) {
        for (MultipartFile image : images) {
            validateSingleImage(image);
        }
    }

    @PostConstruct
    private void normalizeBaseUrl() {
        baseUrl = trimTrailingSlashes(baseUrl);
    }

    /**
     * 단일 이미지 파일의 유효성을 검증합니다.
     */
    private void validateSingleImage(MultipartFile image) {
        // null 체크
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("파일 크기가 너무 큽니다. (최대 %dMB, 현재 %.2fMB)",
                            MAX_FILE_SIZE / (1024 * 1024),
                            image.getSize() / (1024.0 * 1024.0))
            );
        }

        // 파일 형식 검증
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }

        // 파일명 검증
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }
    }

    /**
     * UUID를 사용하여 고유한 파일명을 생성합니다.
     */
    private String generateUniqueFileName(String originalFilename, String directory) {
        String extension = getFileExtension(originalFilename);
        String uniqueId = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());

        return String.format("%s_%s%s", uniqueId, timestamp, extension);
    }

    /**
     * 파일명에서 확장자를 추출합니다.
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    /**
     * 현재 지원하는 이미지 형식 목록을 반환합니다.
     */
    public Set<String> getSupportedContentTypes() {
        return Collections.unmodifiableSet(ALLOWED_CONTENT_TYPES);
    }

    /**
     * 최대 파일 크기를 반환합니다.
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * 최대 이미지 업로드 개수를 반환합니다.
     */
    public int getMaxImageCount() {
        return MAX_IMAGE_COUNT;
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
