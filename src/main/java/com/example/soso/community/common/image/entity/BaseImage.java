package com.example.soso.community.common.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 공통 필드 추상 엔티티
 *
 * <p>자유게시판 이미지(PostImage)와 투표게시판 이미지(PollImage)의
 * 공통 필드(URL, 순서)를 담는다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "image_url", nullable = false, length = 500)
    protected String imageUrl;

    @Column(name = "sequence", nullable = false)
    protected int sequence;
}
