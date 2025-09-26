package com.example.soso.community.common.comment.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Lob
    private String content;

    private int likeCount;

    @Builder.Default
    private boolean deleted = false;

    public void updateContent(String content) {
        if(content != null) {
            this.content = content;
        }
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Comment getParent() {
        return parent;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return super.getCreatedDate();
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return super.getLastModifiedDate();
    }
}
