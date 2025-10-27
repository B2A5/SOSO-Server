package com.example.soso.community.voteboard.domain.entity;

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
    name = "vote_result",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vote_result_user_post",
            columnNames = {"user_id", "vote_post_id"}
        )
    },
    indexes = {
        @Index(name = "idx_vote_result_user", columnList = "user_id"),
        @Index(name = "idx_vote_result_post", columnList = "vote_post_id")
    }
)
public class VoteResult extends BaseTimeEntity {

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
    @JoinColumn(name = "vote_post_id", nullable = false)
    private VotePost votePost;

    /**
     * 선택한 옵션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_option_id", nullable = false)
    private VoteOption voteOption;

    @Builder
    public VoteResult(Users user, VotePost votePost, VoteOption voteOption) {
        this.user = user;
        this.votePost = votePost;
        this.voteOption = voteOption;
    }

    /**
     * 투표 변경 (재투표)
     *
     * @param newOption 새로운 선택 옵션
     */
    public void changeVote(VoteOption newOption) {
        // 기존 옵션 투표수 감소
        this.voteOption.decreaseVoteCount();

        // 새 옵션으로 변경
        this.voteOption = newOption;

        // 새 옵션 투표수 증가
        newOption.increaseVoteCount();
    }
}
