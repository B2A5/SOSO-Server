package com.example.soso.community.voteboard.comment.domain.entity;

import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 투표 게시판 댓글 좋아요 엔티티
 *
 * 한 사용자는 하나의 댓글에 하나의 좋아요만 가능 (unique constraint)
 */
@Entity
@Table(name = "voteboard_comment_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteboardCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private VoteboardComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Builder
    public VoteboardCommentLike(VoteboardComment comment, Users user) {
        this.comment = comment;
        this.user = user;
    }

    /**
     * 좋아요 생성 정적 팩토리 메서드
     */
    public static VoteboardCommentLike create(VoteboardComment comment, Users user) {
        return VoteboardCommentLike.builder()
                .comment(comment)
                .user(user)
                .build();
    }
}
