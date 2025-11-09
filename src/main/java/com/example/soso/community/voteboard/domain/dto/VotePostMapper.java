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
 * 투표 게시글 매퍼
 */
@Component
@RequiredArgsConstructor
public class VotePostMapper {

    private final UserMapper userMapper;

    /**
     * 생성 요청 DTO를 VotePost 엔티티로 변환
     */
    public VotePost toEntity(VotePostCreateRequest request, Users user) {
        VotePost votePost = VotePost.create(
                user,
                request.getTitle(),
                request.getContent(),
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
    public VotePostSummaryResponse toSummaryResponse(VotePost votePost, long commentCount, long likeCount, boolean isLiked) {
        // 투표 옵션 미리보기 (최대 3개)
        List<VoteOptionResponse> voteOptions = votePost.getVoteOptions().stream()
                .limit(3)
                .map(option -> toVoteOptionResponse(option, votePost.getTotalVotes()))
                .toList();

        return VotePostSummaryResponse.builder()
                .id(votePost.getId())
                .title(votePost.getTitle())
                .author(userMapper.toUserSummary(votePost.getUser()))
                .viewCount(votePost.getViewCount())
                .commentCount(commentCount)
                .totalVotes(votePost.getTotalVotes())
                .voteStatus(votePost.getVoteStatus())
                .endTime(votePost.getEndTime())
                .allowRevote(votePost.isAllowRevote())
                .allowMultipleChoice(votePost.isAllowMultipleChoice())
                .voteOptions(voteOptions)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .createdDate(votePost.getCreatedDate())
                .lastModifiedDate(votePost.getLastModifiedDate())
                .build();
    }

    /**
     * VotePost를 상세 응답 DTO로 변환
     */
    public VotePostDetailResponse toDetailResponse(
            VotePost votePost,
            long commentCount,
            List<VoteResult> userVoteResults,
            long likeCount,
            boolean isLiked
    ) {
        // 사용자가 선택한 옵션 ID 목록
        List<Long> selectedOptionIds = userVoteResults != null ?
                userVoteResults.stream()
                        .map(vr -> vr.getVoteOption().getId())
                        .toList() :
                List.of();

        return VotePostDetailResponse.builder()
                .id(votePost.getId())
                .title(votePost.getTitle())
                .content(votePost.getContent())
                .author(userMapper.toUserSummary(votePost.getUser()))
                .imageUrls(votePost.getImages().stream()
                        .map(VotePostImage::getImageUrl)
                        .toList())
                .voteOptions(votePost.getVoteOptions().stream()
                        .map(option -> toVoteOptionResponse(option, votePost.getTotalVotes()))
                        .toList())
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
                .createdDate(votePost.getCreatedDate())
                .lastModifiedDate(votePost.getLastModifiedDate())
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
    public VotePostListResponse toListResponse(
            List<VotePostSummaryResponse> posts,
            String nextCursor,
            boolean hasNext
    ) {
        return VotePostListResponse.builder()
                .posts(posts)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(posts.size())
                .build();
    }
}
