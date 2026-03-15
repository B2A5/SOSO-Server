package com.example.soso.community.pollboard.domain.dto;

import com.example.soso.community.pollboard.domain.entity.PollOption;
import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.community.pollboard.domain.entity.PollImage;
import com.example.soso.community.pollboard.domain.entity.Vote;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 투표 게시판 매퍼
 */
@Component
@RequiredArgsConstructor
public class PollMapper {

    private final UserMapper userMapper;

    /**
     * 생성 요청 DTO를 Poll 엔티티로 변환
     */
    public Poll toEntity(PollCreateRequest request, Users user) {
        Poll poll = Poll.create(
                user,
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getEndTime(),
                request.getAllowRevote(),
                request.getAllowMultipleChoice()
        );

        // 투표 옵션 추가
        List<PollOption> options = request.getVoteOptions().stream()
                .map(optionRequest -> PollOption.builder()
                        .poll(poll)
                        .content(optionRequest.getContent())
                        .sequence(request.getVoteOptions().indexOf(optionRequest))
                        .build())
                .toList();

        poll.addOptions(options);

        return poll;
    }

    /**
     * Poll을 요약 응답 DTO로 변환 (목록 조회용)
     */
    public PollSummary toSummaryResponse(Poll poll, long commentCount, long likeCount, Boolean isLiked, Boolean hasVoted) {
        // 투표 옵션 미리보기 (최대 3개)
        List<PollOptionResponse> voteOptions = poll.getOptions().stream()
                .limit(3)
                .map(option -> toVoteOptionResponse(option, poll.getTotalVotes()))
                .toList();

        // 이미지 정보 추출
        List<PollImage> images = poll.getImages();
        String thumbnailUrl = images.isEmpty() ? null :
                images.stream()
                        .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                        .findFirst()
                        .map(PollImage::getImageUrl)
                        .orElse(null);
        int imageCount = images.size();

        // 내용 미리보기 생성 (100자 제한)
        String contentPreview = poll.getContent() != null && poll.getContent().length() > 100
                ? poll.getContent().substring(0, 100) + "..."
                : poll.getContent();

        // VoteInfo 생성
        VoteInfo voteInfo = new VoteInfo(
                List.of(), // Summary에서는 selectedOptionIds 없음
                poll.getTotalVotes(),
                poll.getPollStatus(),
                poll.getEndTime(),
                poll.isAllowRevote(),
                poll.isAllowMultipleChoice()
        );

        return PollSummary.builder()
                .postId(poll.getId())
                .author(userMapper.toUserSummary(poll.getUser()))
                .category(poll.getCategory())
                .title(poll.getTitle())
                .contentPreview(contentPreview)
                .thumbnailUrl(thumbnailUrl)
                .imageCount(imageCount)
                .viewCount(poll.getViewCount())
                .commentCount(commentCount)
                .hasVoted(hasVoted)
                .voteInfo(voteInfo)
                .voteOptions(voteOptions)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(poll.getCreatedAt())
                .updatedAt(poll.getUpdatedAt())
                .build();
    }

    /**
     * Poll을 상세 응답 DTO로 변환
     */
    public PollDetailResponse toDetailResponse(
            Poll poll,
            long commentCount,
            List<Vote> userVoteResults,
            long likeCount,
            Boolean isLiked,
            String userId
    ) {
        // 인증 여부 확인
        boolean isAuthorized = userId != null;

        // 작성자 여부 확인
        boolean isAuthor = userId != null && poll.getUser().getId().equals(userId);

        // 사용자가 선택한 옵션 ID 목록
        List<Long> selectedOptionIds = userVoteResults != null ?
                userVoteResults.stream()
                        .map(vr -> vr.getVoteOption().getId())
                        .toList() :
                List.of();

        // 투표 참여 여부 계산
        Boolean hasVoted = userId != null
                ? (userVoteResults != null && !userVoteResults.isEmpty())
                : null;

        // 이미지 정보 목록 추출
        List<PollDetailResponse.ImageInfo> images = poll.getImages().stream()
                .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                .map(img -> PollDetailResponse.ImageInfo.builder()
                        .imageId(img.getId())
                        .imageUrl(img.getImageUrl())
                        .sequence(img.getSequence())
                        .build())
                .toList();

        // VoteInfo 생성
        VoteInfo voteInfo = new VoteInfo(
                selectedOptionIds,
                poll.getTotalVotes(),
                poll.getPollStatus(),
                poll.getEndTime(),
                poll.isAllowRevote(),
                poll.isAllowMultipleChoice()
        );

        return PollDetailResponse.builder()
                .postId(poll.getId())
                .author(userMapper.toUserSummary(poll.getUser()))
                .category(poll.getCategory())
                .title(poll.getTitle())
                .content(poll.getContent())
                .images(images)
                .voteOptions(poll.getOptions().stream()
                        .map(option -> toVoteOptionResponse(option, poll.getTotalVotes()))
                        .toList())
                .hasVoted(hasVoted)
                .voteInfo(voteInfo)
                .viewCount(poll.getViewCount())
                .commentCount(commentCount)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .isAuthorized(isAuthorized)
                .isAuthor(isAuthor)
                .canEdit(isAuthorized ? isAuthor : null)
                .canDelete(isAuthorized ? isAuthor : null)
                .createdAt(poll.getCreatedAt())
                .updatedAt(poll.getUpdatedAt())
                .build();
    }

    /**
     * PollOption을 응답 DTO로 변환
     */
    public PollOptionResponse toVoteOptionResponse(PollOption option, int totalVotes) {
        return PollOptionResponse.builder()
                .id(option.getId())
                .content(option.getContent())
                .sequence(option.getSequence())
                .voteCount(option.getVoteCount())
                .percentage(option.calculatePercentage(totalVotes))
                .build();
    }

    /**
     * 목록 응답 생성
     */
    public PollCursorResponse toListResponse(
            List<PollSummary> posts,
            String nextCursor,
            boolean hasNext,
            long totalCount,
            boolean isAuthorized
    ) {
        return PollCursorResponse.builder()
                .posts(posts)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(posts.size())
                .totalCount(totalCount)
                .isAuthorized(isAuthorized)
                .build();
    }
}
