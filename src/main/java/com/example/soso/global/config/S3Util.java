package com.example.soso.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Util {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(String dir, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExt = getFileExtension(originalFilename);
        String fileName = dir + "/" + UUID.randomUUID() + fileExt;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return getFileUrl(fileName);
    }

    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getFileUrl(String key) {
        return "https://" + bucket + ".s3.amazonaws.com/" + key;
    }

    private String extractKeyFromUrl(String fileUrl) {
        String decodedUrl = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);
        return decodedUrl.substring(decodedUrl.indexOf(bucket) + bucket.length() + 1); // after bucket/
    }
}
