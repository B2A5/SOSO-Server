package com.example.soso.community.freeboard.post.domain.entity;

import com.example.soso.community.common.board.entity.BaseBoard;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
public class Post extends BaseBoard {

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<PostImage> images = new ArrayList<>();

    /** 정적 팩토리 메서드 */
    public static Post create(Users user, String title, String content, Category category) {
        return Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .category(category)
                .build();
    }

    public void addImage(PostImage image) {
        this.images.add(image);
        image.setPost(this);
    }

    public void update(String title, String content, Category category, List<PostImage> newImages) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (category != null) this.category = category;
        if (newImages != null && !newImages.isEmpty()) this.images = newImages;
    }
}
