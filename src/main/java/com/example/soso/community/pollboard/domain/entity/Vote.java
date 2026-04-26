package com.example.soso.community.pollboard.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 결과 엔티티
 *
 * 사용자의 투표 기록 저장
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "pollboard_vote_results",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pollboard_vote_results_user_poll_option",
            columnNames = {"user_id", "poll_id", "poll_option_id"}
        )
    },
    indexes = {
        @Index(name = "idx_pollboard_vote_results_user", columnList = "user_id"),
        @Index(name = "idx_pollboard_vote_results_poll", columnList = "poll_id"),
        @Index(name = "idx_pollboard_vote_results_user_poll", columnList = "user_id, poll_id")
    }
)
public class Vote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 투표한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * 투표 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    /**
     * 선택한 옵션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private PollOption voteOption;

    @Builder
    public Vote(Users user, Poll poll, PollOption voteOption) {
        this.user = user;
        this.poll = poll;
        this.voteOption = voteOption;
    }

    /**
     * 투표 변경 (재투표)
     *
     * @param newOption 새로운 선택 옵션
     */
    public void changeVote(PollOption newOption) {
        // 기존 옵션 투표수 감소
        this.voteOption.decreaseVoteCount();

        // 새 옵션으로 변경
        this.voteOption = newOption;

        // 새 옵션 투표수 증가
        newOption.increaseVoteCount();
    }
}
