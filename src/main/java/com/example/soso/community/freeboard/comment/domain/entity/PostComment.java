package com.example.soso.community.freeboard.comment.domain.entity;

import com.example.soso.community.common.comment.entity.BaseComment;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 자유게시판 댓글 엔티티 (구: Comment)
 *
 * <p>BaseComment의 공통 필드(id, user, content, likeCount, deleted)를 상속하며,
 * 자유게시판 게시글 참조(post)와 대댓글 참조(parent)를 추가로 가진다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "comments")
public class PostComment extends BaseComment {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PostComment parent;

    public PostComment getParent() {
        return parent;
    }

    public Post getPost() {
        return post;
    }
}
