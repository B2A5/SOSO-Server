package com.example.soso.global.s3;


import com.example.soso.global.image.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.session.store-type=none",
        "spring.kafka.bootstrap-servers="
})
@Disabled("Docker 환경에서 Testcontainers 사용 불가로 인해 비활성화")
@DisplayName("S3 통합 테스트 - Testcontainers MinIO")
class S3IntegrationTest {

    @Container
    static final MinIOContainer minioContainer = new MinIOContainer(
            DockerImageName.parse("minio/minio:RELEASE.2024-01-16T16-07-38Z"))
            .withUserName("testuser")
            .withPassword("testpass123");

    @Autowired
    private ImageUploadService imageUploadService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Client s3Client;

    private static final String TEST_BUCKET = "test-bucket";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.credentials.access-key", () -> "testuser");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "testpass123");
        registry.add("spring.cloud.aws.s3.endpoint", minioContainer::getS3URL);
        registry.add("spring.cloud.aws.s3.path-style-access", () -> "true");
        registry.add("spring.cloud.aws.s3.bucket", () -> TEST_BUCKET);
        registry.add("spring.cloud.aws.s3.base-url", () -> minioContainer.getS3URL() + "/" + TEST_BUCKET);
        registry.add("spring.cloud.aws.region", () -> "us-east-1");
    }

    @BeforeEach
    void setUp() {
        // 테스트용 버킷 생성 (이미 존재하는 경우 무시)
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(TEST_BUCKET)
                    .build());
        } catch (software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException e) {
            // 버킷이 이미 존재하는 경우 무시
        }
    }

    @Test
    @DisplayName("단일 이미지 업로드 및 URL 생성 테스트")
    void uploadSingleImage_Success() {
        // given
        MockMultipartFile testImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when
        String imageUrl = imageUploadService.uploadSingleImage(testImage, "test");

        // then
        assertThat(imageUrl).isNotNull();
        assertThat(imageUrl).contains(TEST_BUCKET);
        assertThat(imageUrl).contains("test/");
        assertThat(imageUrl).endsWith(".jpg");

        // S3에 실제로 업로드되었는지 확인
        String key = imageUrl.substring(imageUrl.indexOf("test/"));
        assertThatCode(() -> s3Client.headObject(HeadObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(key)
                .build())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 이미지 일괄 업로드 테스트")
    void uploadMultipleImages_Success() {
        // given
        MockMultipartFile image1 = new MockMultipartFile(
                "image1", "test1.jpg", "image/jpeg", "test image 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile(
                "image2", "test2.png", "image/png", "test image 2".getBytes());
        List<MultipartFile> images = List.of(image1, image2);

        // when
        List<String> imageUrls = imageUploadService.uploadImages(images, "freeboard");

        // then
        assertThat(imageUrls).hasSize(2);
        assertThat(imageUrls.get(0)).contains("freeboard/");
        assertThat(imageUrls.get(1)).contains("freeboard/");

        // S3에 실제로 업로드되었는지 확인
        for (String url : imageUrls) {
            String key = url.substring(url.indexOf("freeboard/"));
            assertThatCode(() -> s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(TEST_BUCKET)
                    .key(key)
                    .build())).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("이미지 삭제 테스트")
    void deleteImage_Success() {
        // given - 먼저 이미지 업로드
        MockMultipartFile testImage = new MockMultipartFile(
                "image", "delete-test.jpg", "image/jpeg", "delete test content".getBytes());
        String imageUrl = imageUploadService.uploadSingleImage(testImage, "delete-test");

        // when
        imageUploadService.deleteImage(imageUrl);

        // then - S3에서 삭제되었는지 확인
        String key = imageUrl.substring(imageUrl.indexOf("delete-test/"));
        assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(key)
                .build())).isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("여러 이미지 일괄 삭제 테스트")
    void deleteMultipleImages_Success() {
        // given
        MockMultipartFile image1 = new MockMultipartFile(
                "image1", "bulk1.jpg", "image/jpeg", "bulk test 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile(
                "image2", "bulk2.jpg", "image/jpeg", "bulk test 2".getBytes());
        List<MultipartFile> imagesToDelete = List.of(image1, image2);
        List<String> uploadedUrls = imageUploadService.uploadImages(imagesToDelete, "bulk-delete");

        // when
        imageUploadService.deleteImages(uploadedUrls);

        // then
        for (String url : uploadedUrls) {
            String key = url.substring(url.indexOf("bulk-delete/"));
            assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(TEST_BUCKET)
                    .key(key)
                    .build())).isInstanceOf(NoSuchKeyException.class);
        }
    }

    @Test
    @DisplayName("S3Service - 이미지 존재 확인 테스트")
    void isImageExists_Test() {
        // given - ImageUploadService를 통해 이미지 업로드
        MockMultipartFile testImage = new MockMultipartFile(
                "image", "exists-test.jpg", "image/jpeg", "exists test".getBytes());
        String imageUrl = imageUploadService.uploadSingleImage(testImage, "exists-test");

        // when & then
        assertThat(s3Service.isImageExists(imageUrl)).isTrue();

        // 삭제 후 존재 확인
        s3Service.deleteImage(imageUrl);
        assertThat(s3Service.isImageExists(imageUrl)).isFalse();
    }

    @Test
    @DisplayName("잘못된 파일 형식 업로드 실패 테스트")
    void uploadInvalidFileType_ShouldThrowException() {
        // given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "invalid file content".getBytes());

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadSingleImage(invalidFile, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 파일 형식");
    }

    @Test
    @DisplayName("파일 크기 제한 테스트")
    void uploadLargeFile_ShouldThrowException() {
        // given - 6MB 파일 생성 (제한: 5MB)
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", largeContent);

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadSingleImage(largeFile, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 크기가 너무 큽니다");
    }

    @Test
    @DisplayName("최대 이미지 개수 제한 테스트")
    void uploadTooManyImages_ShouldThrowException() {
        // given - 5개 이미지 (제한: 4개)
        List<MultipartFile> tooManyImages = List.of(
                new MockMultipartFile("img1", "1.jpg", "image/jpeg", "content1".getBytes()),
                new MockMultipartFile("img2", "2.jpg", "image/jpeg", "content2".getBytes()),
                new MockMultipartFile("img3", "3.jpg", "image/jpeg", "content3".getBytes()),
                new MockMultipartFile("img4", "4.jpg", "image/jpeg", "content4".getBytes()),
                new MockMultipartFile("img5", "5.jpg", "image/jpeg", "content5".getBytes())
        );

        // when & then
        assertThatThrownBy(() -> imageUploadService.uploadImages(tooManyImages, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지는 최대 4장까지");
    }

    @Test
    @DisplayName("잘못된 URL로 삭제 시도 테스트")
    void deleteInvalidUrl_ShouldNotThrowException() {
        // given
        String invalidUrl = "https://wrong-domain.com/invalid.jpg";

        // when & then - 예외 없이 조용히 무시되어야 함
        assertThatCode(() -> imageUploadService.deleteImage(invalidUrl))
                .doesNotThrowAnyException();
    }
}