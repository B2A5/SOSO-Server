package com.example.soso.community.pollboard.domain.entity;

import com.example.soso.community.common.board.entity.BaseBoard;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 투표 게시글 엔티티
 *
 * Post와 유사하지만 투표 기능이 추가된 독립 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "votesboard")  // DB 테이블명은 유지 (마이그레이션 없음)
public class Poll extends BaseBoard {

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<PollImage> images = new ArrayList<>();

    /**
     * 투표 옵션 목록 (2-5개)
     */
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<PollOption> options = new ArrayList<>();

    /**
     * 투표 마감 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 투표 후 수정 가능 여부
     * true: 투표 후 변경 가능
     * false: 투표 후 변경 불가 (기본값)
     */
    @Column(name = "allow_revote", nullable = false)
    private boolean allowRevote = false;

    /**
     * 중복 선택 허용 여부
     * true: 여러 옵션 동시 선택 가능 (최대 n-1개)
     * false: 하나의 옵션만 선택 가능 (기본값)
     */
    @Column(name = "allow_multiple_choice", nullable = false)
    private boolean allowMultipleChoice = false;

    /**
     * 총 투표 참여자 수
     */
    @Column(name = "total_votes", nullable = false)
    private int totalVotes = 0;

    /**
     * Poll 생성 정적 팩토리 메서드
     */
    public static Poll create(Users user, String title, String content, Category category,
                              LocalDateTime endTime, boolean allowRevote, boolean allowMultipleChoice) {
        Poll poll = new Poll();
        poll.user = user;
        poll.title = title;
        poll.content = content;
        poll.category = category;
        poll.endTime = endTime;
        poll.allowRevote = allowRevote;
        poll.allowMultipleChoice = allowMultipleChoice;
        poll.totalVotes = 0;
        poll.viewCount = 0;
        poll.deleted = false;
        return poll;
    }

    /**
     * 투표 옵션 추가
     */
    public void addOption(PollOption option) {
        this.options.add(option);
        option.setPoll(this);
    }

    /**
     * 투표 옵션 리스트 추가
     */
    public void addOptions(List<PollOption> pollOptions) {
        for (PollOption option : pollOptions) {
            addOption(option);
        }
    }

    /**
     * 투표 참여자 수 증가
     */
    public void increaseTotalVotes() {
        this.totalVotes++;
    }

    /**
     * 투표 참여자 수 감소 (재투표 시)
     */
    public void decreaseTotalVotes() {
        if (this.totalVotes > 0) {
            this.totalVotes--;
        }
    }

    /**
     * 투표 진행 중 여부 확인
     *
     * @return true: 진행 중, false: 완료
     */
    public boolean isActive() {
        return LocalDateTime.now().isBefore(endTime) && !isDeleted();
    }

    /**
     * 투표 상태 반환
     */
    public PollStatus getPollStatus() {
        if (isDeleted()) {
            return PollStatus.DELETED;
        }
        return isActive() ? PollStatus.IN_PROGRESS : PollStatus.COMPLETED;
    }

    /**
     * 이미지 추가
     */
    public void addImage(PollImage image) {
        this.images.add(image);
        image.setPoll(this);
    }

    /**
     * 게시글 수정 (투표 옵션 제외)
     */
    public void updatePost(String title, String content, Category category) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
    }

    /**
     * 투표 설정 수정
     */
    public void updateVoteSettings(LocalDateTime endTime, boolean allowRevote, boolean allowMultipleChoice) {
        if (this.totalVotes == 0) {
            this.endTime = endTime;
            this.allowRevote = allowRevote;
            this.allowMultipleChoice = allowMultipleChoice;
        }
    }
}
