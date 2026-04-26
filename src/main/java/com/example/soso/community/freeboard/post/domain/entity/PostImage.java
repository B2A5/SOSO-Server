package com.example.soso.community.freeboard.post.domain.entity;

import com.example.soso.community.common.image.entity.BaseImage;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "freeboard_post_images")
public class PostImage extends BaseImage {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public void setPost(Post post) {
        this.post = post;
    }

    @Builder
    public PostImage(String imageUrl, int sequence, Post post) {
        this.imageUrl = imageUrl;
        this.sequence = sequence;
        this.post = post;
    }
}
