package com.example.soso.community.voteboard.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 게시글 이미지 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vote_post_image")
public class VotePostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_post_id", nullable = false)
    private VotePost votePost;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Builder
    public VotePostImage(VotePost votePost, String imageUrl, int sequence) {
        this.votePost = votePost;
        this.imageUrl = imageUrl;
        this.sequence = sequence;
    }

    public void setVotePost(VotePost votePost) {
        this.votePost = votePost;
    }
}
