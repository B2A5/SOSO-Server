package com.example.soso.community.voteboard.domain.entity;

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
 * 투표 게시글 좋아요 엔티티
 *
 * 한 사용자는 하나의 투표 게시글에 하나의 좋아요만 가능 (unique constraint)
 */
@Entity
@Table(name = "vote_post_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vote_post_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VotePostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_post_id", nullable = false)
    private VotePost votePost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Builder
    public VotePostLike(VotePost votePost, Users user) {
        this.votePost = votePost;
        this.user = user;
    }

    /**
     * 좋아요 생성 정적 팩토리 메서드
     */
    public static VotePostLike create(VotePost votePost, Users user) {
        return VotePostLike.builder()
                .votePost(votePost)
                .user(user)
                .build();
    }
}
