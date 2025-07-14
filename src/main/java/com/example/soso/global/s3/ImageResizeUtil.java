package com.example.soso.global.s3;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 이미지 리사이징 유틸
 * - 업로드된 이미지를 지정된 너비로 줄이고, jpg 형식으로 변환
 * - 기본 너비는 1024px (세로는 비율 유지)
 */
public class ImageResizeUtil {

    private static final int DEFAULT_WIDTH = 1024;

    /**
     * 이미지 파일을 리사이징하여 jpg byte[]로 반환
     * @param file 업로드된 MultipartFile
     * @param width 리사이징할 너비 (세로는 자동 비율 조정)
     * @return jpg 포맷의 byte 배열
     * @throws IOException 변환 실패 시 예외
     */
    public static byte[] resizeToJpg(MultipartFile file, int width) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 썸네일 라이브러리를 사용하여 이미지 리사이징 및 포맷 변환
        Thumbnails.of(file.getInputStream())
                .size(width, width)              // 너비 기준, 세로는 자동 비율 유지
                .outputFormat("jpg")             // 무조건 jpg로 변환
                .outputQuality(0.85)             // 화질 압축률 (0.0 ~ 1.0)
                .toOutputStream(outputStream);   // 결과를 바이트 배열로 저장

        return outputStream.toByteArray();
    }

    /**
     * 기본 너비(1024px)로 리사이징
     */
    public static byte[] resizeToJpg(MultipartFile file) throws IOException {
        return resizeToJpg(file, DEFAULT_WIDTH);
    }
}
