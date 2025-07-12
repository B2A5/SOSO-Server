package com.example.soso.global.config;

import com.example.soso.global.config.ImageResizeUtil;
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

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;

    /**
     * 리사이징된 jpg 이미지 S3에 업로드
     * @param file MultipartFile (업로드된 원본 파일)
     * @param dir  디렉토리 (예: posts, ideas 등)
     * @return S3에 저장된 이미지 URL
     */
    public String uploadImage(MultipartFile file, String dir) {
        try {
            byte[] resizedImage = ImageResizeUtil.resizeToJpg(file);
            String fileName = dir + "/" + UUID.randomUUID() + ".jpg";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType("image/jpeg")
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(resizedImage));
            return baseUrl + "/" + fileName;

        } catch (IOException e) {
            log.error("S3 이미지 업로드 실패", e);
            throw new RuntimeException("이미지 업로드 중 오류 발생", e);
        }
    }

    /**
     * 이미지 삭제
     * @param imageUrl 삭제할 이미지의 S3 URL
     */
    public void deleteImage(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);

        s3Client.deleteObject(builder -> builder
                .bucket(bucket)
                .key(key)
        );
    }

    /**
     * 이미지 존재 여부 확인
     * @param imageUrl 확인할 이미지의 S3 URL
     * @return 존재 여부
     */
    public boolean isImageExists(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        try {
            s3Client.headObject(builder -> builder
                    .bucket(bucket)
                    .key(key)
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("이미지 존재 확인 중 예외 발생", e);
            return false;
        }
    }

    /**
     * S3 full URL에서 key만 추출
     * ex) https://bucket.s3.amazonaws.com/posts/uuid.jpg → posts/uuid.jpg
     */
    private String extractKeyFromUrl(String url) {
        return url.replace(baseUrl + "/", "");
    }
}
