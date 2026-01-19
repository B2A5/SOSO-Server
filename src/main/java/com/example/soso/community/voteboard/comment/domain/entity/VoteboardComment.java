package com.example.soso.community.voteboard.comment.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.community.voteboard.domain.entity.Votesboard;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 게시판 댓글 엔티티
 *
 * 특징:
 * - Votesboard와 연결
 * - 대댓글 지원 (parent 관계)
 * - 소프트 삭제 지원
 * - 좋아요 수 포함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "voteboard_comments")
public class VoteboardComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votesboard_id", nullable = false)
    private Votesboard votesboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private VoteboardComment parent;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * 댓글 내용 수정
     */
    public void updateContent(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }

    /**
     * 소프트 삭제
     */
    public void delete() {
        this.deleted = true;
        this.content = "삭제된 댓글입니다";
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 부모 댓글 가져오기
     */
    public VoteboardComment getParent() {
        return parent;
    }

    /**
     * 생성일시 가져오기
     */
    public java.time.LocalDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    /**
     * 수정일시 가져오기
     */
    public java.time.LocalDateTime getUpdatedAt() {
        return super.getUpdatedAt();
    }
}
