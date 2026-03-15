package com.example.soso.community.pollboard.comment.domain.entity;

import com.example.soso.community.common.comment.entity.BaseComment;
import com.example.soso.community.pollboard.domain.entity.Poll;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 투표 게시판 댓글 엔티티
 *
 * 특징:
 * - Poll과 연결
 * - 대댓글 지원 (parent 관계)
 * - 소프트 삭제 지원
 * - 좋아요 수 포함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@Entity
@Table(name = "votesboard_comments")
public class PollComment extends BaseComment {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votesboard_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PollComment parent;

    /**
     * 부모 댓글 가져오기
     */
    public PollComment getParent() {
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
