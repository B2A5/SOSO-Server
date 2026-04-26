package com.example.soso.community.pollboard.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 옵션 엔티티
 *
 * 각 투표 게시글의 선택 가능한 옵션
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pollboard_vote_options")
public class PollOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 투표 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /**
     * 옵션 내용
     */
    @Column(name = "content", nullable = false, length = 100)
    private String content;

    /**
     * 옵션 순서 (0부터 시작)
     */
    @Column(name = "sequence", nullable = false)
    private int sequence;

    /**
     * 이 옵션에 투표한 사람 수
     */
    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Builder
    public PollOption(Poll poll, String content, int sequence) {
        this.poll = poll;
        this.content = content;
        this.sequence = sequence;
        this.voteCount = 0;
    }

    /**
     * Poll 설정 (양방향 관계 설정용)
     */
    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    /**
     * 투표 수 증가
     */
    public void increaseVoteCount() {
        this.voteCount++;
    }

    /**
     * 투표 수 감소 (재투표 시)
     */
    public void decreaseVoteCount() {
        if (this.voteCount > 0) {
            this.voteCount--;
        }
    }

    /**
     * 투표 비율 계산
     *
     * @param totalVotes 전체 투표 수
     * @return 투표 비율 (0-100)
     */
    public double calculatePercentage(int totalVotes) {
        if (totalVotes == 0) {
            return 0.0;
        }
        return (double) this.voteCount / totalVotes * 100.0;
    }
}
