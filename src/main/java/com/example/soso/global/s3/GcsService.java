package com.example.soso.global.s3;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "provider", havingValue = "gcs")
public class GcsService {

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucket;

    @Value("${spring.cloud.gcp.storage.base-url}")
    private String baseUrl;

    public String uploadImage(MultipartFile file, String dir) {
        try {
            byte[] resizedImage = ImageResizeUtil.resizeToJpg(file);
            String fileName = dir + "/" + UUID.randomUUID() + ".jpg";

            BlobId blobId = BlobId.of(bucket, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/jpeg")
                    .build();

            storage.create(blobInfo, resizedImage);

            return baseUrl + "/" + fileName;

        } catch (IOException e) {
            log.error("GCS 이미지 업로드 실패", e);
            throw new RuntimeException("이미지 업로드 중 오류 발생", e);
        }
    }

    public void deleteImage(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        boolean deleted = storage.delete(bucket, key);
        if (!deleted) {
            log.warn("삭제 실패: 객체가 존재하지 않음 - {}", key);
        }
    }

    public boolean isImageExists(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        return storage.get(bucket, key) != null;
    }

    private String extractKeyFromUrl(String url) {
        return url.replace(baseUrl + "/", "");
    }
}
