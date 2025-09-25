package com.example.soso.community.common.post.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Users user;

    @Enumerated(EnumType.STRING)
    private Category category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    private String title;

    @Lob
    private String content;

    private int likeCount;
    private int commentCount;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void delete() {
        this.deleted = true;
    }


    public void addImage(PostImage image) {
        this.images.add(image);
        image.setPost(this);
    }

    public void update(String title, String content, Category category, List<PostImage> newImages) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
        if (newImages != null && !newImages.isEmpty()) {
            this.images = newImages;
        }
    }

    public void updateLikeCount(int redisLikeCount) {
        this.likeCount = redisLikeCount;
    }

    public void updateCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public LocalDateTime getCreatedAt() {
        return super.getCreatedDate();
    }

    public LocalDateTime getUpdatedAt() {
        return super.getLastModifiedDate();
    }
}
