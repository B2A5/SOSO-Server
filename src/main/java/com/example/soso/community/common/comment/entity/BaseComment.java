package com.example.soso.community.common.comment.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.Column;
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
 * 댓글 공통 필드 추상 엔티티
 *
 * <p>자유게시판 댓글(PostComment)과 투표게시판 댓글(PollComment) 모두
 * 이 클래스를 상속한다. 부모 댓글(parent) 참조는 구체 타입에 따라
 * 각 하위 클래스에서 선언한다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@MappedSuperclass
public abstract class BaseComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    protected Users user;

    @Lob
    protected String content;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected int likeCount = 0;

    @lombok.Builder.Default
    @Column(nullable = false)
    protected boolean deleted = false;

    public void updateContent(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    public void delete() {
        this.deleted = true;
        this.content = "삭제된 댓글입니다.";
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public boolean isDeleted() {
        return this.deleted;
    }
}
