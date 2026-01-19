package com.example.soso.community.votesboard.domain.entity;

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
@Table(name = "votesboard_image")
public class VotesboardImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votesboard_id", nullable = false)
    private Votesboard votesboard;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Builder
    public VotesboardImage(Votesboard votesboard, String imageUrl, int sequence) {
        this.votesboard = votesboard;
        this.imageUrl = imageUrl;
        this.sequence = sequence;
    }

    public void setVotesboard(Votesboard votesboard) {
        this.votesboard = votesboard;
    }
}
