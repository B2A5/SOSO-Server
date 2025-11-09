package com.example.soso.community.voteboard.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import com.example.soso.users.domain.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Table(name = "vote_post")
public class VotePost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<VotePostImage> images = new ArrayList<>();

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    /**
     * 투표 옵션 목록 (2-5개)
     */
    @OneToMany(mappedBy = "votePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<VoteOption> voteOptions = new ArrayList<>();

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
     * VotePost 생성 정적 팩토리 메서드
     */
    public static VotePost create(Users user, String title, String content,
                                   LocalDateTime endTime, boolean allowRevote, boolean allowMultipleChoice) {
        VotePost votePost = new VotePost();
        votePost.user = user;
        votePost.title = title;
        votePost.content = content;
        votePost.endTime = endTime;
        votePost.allowRevote = allowRevote;
        votePost.allowMultipleChoice = allowMultipleChoice;
        votePost.totalVotes = 0;
        votePost.viewCount = 0;
        votePost.deleted = false;
        return votePost;
    }

    /**
     * 투표 옵션 추가
     */
    public void addVoteOption(VoteOption option) {
        this.voteOptions.add(option);
        option.setVotePost(this);
    }

    /**
     * 투표 옵션 리스트 추가
     */
    public void addVoteOptions(List<VoteOption> options) {
        for (VoteOption option : options) {
            addVoteOption(option);
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
    public VoteStatus getVoteStatus() {
        if (isDeleted()) {
            return VoteStatus.DELETED;
        }
        return isActive() ? VoteStatus.IN_PROGRESS : VoteStatus.COMPLETED;
    }

    /**
     * 이미지 추가
     */
    public void addImage(VotePostImage image) {
        this.images.add(image);
        image.setVotePost(this);
    }

    /**
     * 게시글 수정 (투표 옵션 제외)
     */
    public void updatePost(String title, String content, List<VotePostImage> newImages) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (newImages != null && !newImages.isEmpty()) {
            this.images.clear();
            this.images.addAll(newImages);
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

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 게시글 삭제 (소프트 삭제)
     */
    public void delete() {
        this.deleted = true;
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return this.deleted;
    }
}
