package com.example.soso.community.common.like.entity;

import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 좋아요 공통 필드 추상 엔티티
 *
 * <p>게시글 좋아요(PostLike, PollLike)와 댓글 좋아요(PostCommentLike, PollCommentLike)의
 * 공통 필드를 담는다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    protected Users user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;
}
