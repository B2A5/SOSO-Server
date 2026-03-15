package com.example.soso.community.pollboard.domain.entity;

import com.example.soso.community.common.image.entity.BaseImage;
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
public class PollImage extends BaseImage {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votesboard_id", nullable = false)
    private Poll poll;

    @Builder
    public PollImage(Poll poll, String imageUrl, int sequence) {
        this.poll = poll;
        this.imageUrl = imageUrl;
        this.sequence = sequence;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }
}
