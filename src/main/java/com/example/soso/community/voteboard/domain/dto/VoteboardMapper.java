package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.voteboard.domain.entity.VoteOption;
import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.community.voteboard.domain.entity.VotePostImage;
import com.example.soso.community.voteboard.domain.entity.VoteResult;
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
public class VoteboardMapper {

    private final UserMapper userMapper;

    /**
     * 생성 요청 DTO를 VotePost 엔티티로 변환
     */
    public VotePost toEntity(VoteboardCreateRequest request, Users user) {
        VotePost votePost = VotePost.create(
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
                        .votePost(votePost)
                        .content(optionRequest.getContent())
                        .sequence(request.getVoteOptions().indexOf(optionRequest))
                        .build())
                .toList();

        votePost.addVoteOptions(options);

        return votePost;
    }

    /**
     * VotePost를 요약 응답 DTO로 변환 (목록 조회용)
     */
    public VoteboardSummary toSummaryResponse(VotePost votePost, long commentCount, long likeCount, Boolean isLiked, Boolean hasVoted) {
        // 투표 옵션 미리보기 (최대 3개)
        List<VoteOptionResponse> voteOptions = votePost.getVoteOptions().stream()
                .limit(3)
                .map(option -> toVoteOptionResponse(option, votePost.getTotalVotes()))
                .toList();

        // 이미지 정보 추출
        List<VotePostImage> images = votePost.getImages();
        String thumbnailUrl = images.isEmpty() ? null :
                images.stream()
                        .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                        .findFirst()
                        .map(VotePostImage::getImageUrl)
                        .orElse(null);
        int imageCount = images.size();

        // 내용 미리보기 생성 (100자 제한)
        String contentPreview = votePost.getContent() != null && votePost.getContent().length() > 100
                ? votePost.getContent().substring(0, 100) + "..."
                : votePost.getContent();

        return VoteboardSummary.builder()
                .postId(votePost.getId())
                .author(userMapper.toUserSummary(votePost.getUser()))
                .category(votePost.getCategory())
                .title(votePost.getTitle())
                .contentPreview(contentPreview)
                .thumbnailUrl(thumbnailUrl)
                .imageCount(imageCount)
                .viewCount(votePost.getViewCount())
                .commentCount(commentCount)
                .totalVotes(votePost.getTotalVotes())
                .hasVoted(hasVoted)
                .voteStatus(votePost.getVoteStatus())
                .endTime(votePost.getEndTime())
                .allowRevote(votePost.isAllowRevote())
                .allowMultipleChoice(votePost.isAllowMultipleChoice())
                .voteOptions(voteOptions)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdAt(votePost.getCreatedAt())
                .updatedAt(votePost.getUpdatedAt())
                .build();
    }

    /**
     * VotePost를 상세 응답 DTO로 변환
     */
    public VoteboardDetailResponse toDetailResponse(
            VotePost votePost,
            long commentCount,
            List<VoteResult> userVoteResults,
            long likeCount,
            Boolean isLiked,
            String userId
    ) {
        // 인증 여부 확인
        boolean isAuthorized = userId != null;

        // 작성자 여부 확인
        boolean isAuthor = userId != null && votePost.getUser().getId().equals(userId);

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
        List<VoteboardDetailResponse.ImageInfo> images = votePost.getImages().stream()
                .sorted((img1, img2) -> Integer.compare(img1.getSequence(), img2.getSequence()))
                .map(img -> VoteboardDetailResponse.ImageInfo.builder()
                        .imageId(img.getId())
                        .imageUrl(img.getImageUrl())
                        .sequence(img.getSequence())
                        .build())
                .toList();

        return VoteboardDetailResponse.builder()
                .postId(votePost.getId())
                .author(userMapper.toUserSummary(votePost.getUser()))
                .category(votePost.getCategory())
                .title(votePost.getTitle())
                .content(votePost.getContent())
                .images(images)
                .voteOptions(votePost.getVoteOptions().stream()
                        .map(option -> toVoteOptionResponse(option, votePost.getTotalVotes()))
                        .toList())
                .hasVoted(hasVoted)
                .selectedOptionIds(selectedOptionIds)
                .totalVotes(votePost.getTotalVotes())
                .voteStatus(votePost.getVoteStatus())
                .endTime(votePost.getEndTime())
                .allowRevote(votePost.isAllowRevote())
                .allowMultipleChoice(votePost.isAllowMultipleChoice())
                .viewCount(votePost.getViewCount())
                .commentCount(commentCount)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .isAuthorized(isAuthorized)
                .isAuthor(isAuthor)
                .canEdit(isAuthorized ? isAuthor : null)
                .canDelete(isAuthorized ? isAuthor : null)
                .createdAt(votePost.getCreatedAt())
                .updatedAt(votePost.getUpdatedAt())
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
    public VoteboardCursorResponse toListResponse(
            List<VoteboardSummary> posts,
            String nextCursor,
            boolean hasNext,
            long totalCount,
            boolean isAuthorized
    ) {
        return VoteboardCursorResponse.builder()
                .posts(posts)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(posts.size())
                .totalCount(totalCount)
                .isAuthorized(isAuthorized)
                .build();
    }
}
