package com.example.soso.community.common.board.entity;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 게시판(Board) 공통 필드 추상 엔티티
 *
 * <p>자유게시판(Post)과 투표게시판(Poll) 모두 이 클래스를 상속하여
 * 공통 필드(제목, 내용, 카테고리, 조회수 등)를 재사용한다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@MappedSuperclass
public abstract class BaseBoard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    protected Users user;

    @Column(nullable = false, length = 100)
    protected String title;

    @Lob
    @Column(nullable = false)
    protected String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    protected Category category;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected int viewCount = 0;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected int likeCount = 0;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected int commentCount = 0;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected boolean deleted = false;

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void updateLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void updateCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return this.deleted;
    }
}
