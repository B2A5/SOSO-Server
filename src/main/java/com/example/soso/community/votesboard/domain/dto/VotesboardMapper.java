package com.example.soso.community.votesboard.domain.dto;

import com.example.soso.community.votesboard.domain.entity.VoteOption;
import com.example.soso.community.votesboard.domain.entity.Votesboard;
import com.example.soso.community.votesboard.domain.entity.VotesboardImage;
import com.example.soso.community.votesboard.domain.entity.VoteResult;
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
public class VotesboardMapper {

    private final UserMapper userMapper;

    /**
     * 생성 요청 DTO를 Votesboard 엔티티로 변환
     */
    public Votesboard toEntity(VotesboardCreateRequest request, Users user) {
        Votesboard votesboard = Votesboard.create(
                user,
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getEndTime(),
                request.getAllowRevote(),
                request.getAllowMultipleChoice()
        );

        // 투표 옵션 추가
        List<VoteOption> options = request.getVoteOptions().stream()
                .map(optionRequest -> VoteOption.builder()
                        .votesboard(votesboard)
                        .content(optionRequest.getContent())
                        .sequence(request.getVoteOptions().indexOf(optionRequest))
                        .build())
                .toList();

        votesboard.addVoteOptions(options);

        return votesboard;
    }

    /**
     * Votesboard를 요약 응답 DTO로 변환 (목록 조회용)
     */
    public VotesboardSummary toSummaryResponse(Votesboard votesboard, long commentCount, long likeCount, Boolean isLiked, Boolean hasVoted) {
        // 투표 옵션 미리보기 (최대 3개)
        List<VoteOptionResponse> voteOptions = votesboard.getVoteOptions().stream()
                .limit(3)
                .map(option -> toVoteOptionResponse(option, votesboard.getTotalVotes()))
                .toList();

        // 이미지 정보 추출
        List<VotesboardImage> images = votesboard.getImages();
        String thumbnailUrl = images.isEmpty() ? null :
                images.stream()
                        .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                        .findFirst()
                        .map(VotesboardImage::getImageUrl)
                        .orElse(null);
        int imageCount = images.size();

        // 내용 미리보기 생성 (100자 제한)
        String contentPreview = votesboard.getContent() != null && votesboard.getContent().length() > 100
                ? votesboard.getContent().substring(0, 100) + "..."
                : votesboard.getContent();

        // VoteInfo 생성
        VoteInfo voteInfo = new VoteInfo(
                List.of(), // Summary에서는 selectedOptionIds 없음
                votesboard.getTotalVotes(),
                votesboard.getVoteStatus(),
                votesboard.getEndTime(),
                votesboard.isAllowRevote(),
                votesboard.isAllowMultipleChoice()
        );

        return VotesboardSummary.builder()
                .postId(votesboard.getId())
                .author(userMapper.toUserSummary(votesboard.getUser()))
                .category(votesboard.getCategory())
                .title(votesboard.getTitle())
                .contentPreview(contentPreview)
                .thumbnailUrl(thumbnailUrl)
                .imageCount(imageCount)
                .viewCount(votesboard.getViewCount())
                .commentCount(commentCount)
                .hasVoted(hasVoted)
                .voteInfo(voteInfo)
                .voteOptions(voteOptions)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(votesboard.getCreatedAt())
                .updatedAt(votesboard.getUpdatedAt())
                .build();
    }

    /**
     * Votesboard를 상세 응답 DTO로 변환
     */
    public VotesboardDetailResponse toDetailResponse(
            Votesboard votesboard,
            long commentCount,
            List<VoteResult> userVoteResults,
            long likeCount,
            Boolean isLiked,
            String userId
    ) {
        // 인증 여부 확인
        boolean isAuthorized = userId != null;

        // 작성자 여부 확인
        boolean isAuthor = userId != null && votesboard.getUser().getId().equals(userId);

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
        List<VotesboardDetailResponse.ImageInfo> images = votesboard.getImages().stream()
                .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                .map(img -> VotesboardDetailResponse.ImageInfo.builder()
                        .imageId(img.getId())
                        .imageUrl(img.getImageUrl())
                        .sequence(img.getSequence())
                        .build())
                .toList();

        // VoteInfo 생성
        VoteInfo voteInfo = new VoteInfo(
                selectedOptionIds,
                votesboard.getTotalVotes(),
                votesboard.getVoteStatus(),
                votesboard.getEndTime(),
                votesboard.isAllowRevote(),
                votesboard.isAllowMultipleChoice()
        );

        return VotesboardDetailResponse.builder()
                .postId(votesboard.getId())
                .author(userMapper.toUserSummary(votesboard.getUser()))
                .category(votesboard.getCategory())
                .title(votesboard.getTitle())
                .content(votesboard.getContent())
                .images(images)
                .voteOptions(votesboard.getVoteOptions().stream()
                        .map(option -> toVoteOptionResponse(option, votesboard.getTotalVotes()))
                        .toList())
                .hasVoted(hasVoted)
                .voteInfo(voteInfo)
                .viewCount(votesboard.getViewCount())
                .commentCount(commentCount)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .isAuthorized(isAuthorized)
                .isAuthor(isAuthor)
                .canEdit(isAuthorized ? isAuthor : null)
                .canDelete(isAuthorized ? isAuthor : null)
                .createdAt(votesboard.getCreatedAt())
                .updatedAt(votesboard.getUpdatedAt())
                .build();
    }

    /**
     * VoteOption을 응답 DTO로 변환
     */
    public VoteOptionResponse toVoteOptionResponse(VoteOption option, int totalVotes) {
        return VoteOptionResponse.builder()
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
    public VotesboardCursorResponse toListResponse(
            List<VotesboardSummary> posts,
            String nextCursor,
            boolean hasNext,
            long totalCount,
            boolean isAuthorized
    ) {
        return VotesboardCursorResponse.builder()
                .posts(posts)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(posts.size())
                .totalCount(totalCount)
                .isAuthorized(isAuthorized)
                .build();
    }
}
