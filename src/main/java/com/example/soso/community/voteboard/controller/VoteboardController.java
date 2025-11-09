package com.example.soso.community.voteboard.controller;

import com.example.soso.community.voteboard.domain.dto.*;
import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import com.example.soso.community.voteboard.service.VotePostService;
import com.example.soso.global.exception.domain.ErrorResponse;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 투표 게시판 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/community/votesboard")
@RequiredArgsConstructor
@Tag(name = "Voteboard", description = "투표 게시판 API")
public class VoteboardController {

    private final VotePostService votePostService;

    @PostMapping
    @Operation(
            operationId = "createVotePost",
            summary = "투표 게시글 작성",
            description = """
                    새로운 투표 게시글을 작성합니다.

                    **제약사항:**
                    - 투표 옵션: 최소 2개, 최대 5개
                    - 제목: 최대 100자
                    - 내용: 최대 5000자
                    - 이미지: 최대 5개
                    - 마감 시간: 현재 시간보다 미래여야 함

                    **권한:** 로그인 사용자만 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "투표 게시글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotePostIdResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\"votesboardId\": 1}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "옵션 개수 부족",
                                            value = "{\"code\": \"BAD_REQUEST\", \"message\": \"투표 옵션은 최소 2개, 최대 5개까지 가능합니다.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "과거 마감 시간",
                                            value = "{\"code\": \"BAD_REQUEST\", \"message\": \"투표 마감 시간은 미래 시간이어야 합니다.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "필수 필드 누락",
                                            value = "{\"code\": \"BAD_REQUEST\", \"message\": \"제목은 필수입니다.\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 실패",
                                    value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증되지 않은 사용자입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<VotePostIdResponse> createVotePost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "투표 게시글 생성 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotePostCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "투표 생성 예시",
                                    value = """
                                            {
                                              "title": "점심 메뉴 투표",
                                              "content": "오늘 점심 뭐 먹을까요?",
                                              "imageUrls": ["https://example.com/food.jpg"],
                                              "voteOptions": [
                                                {"content": "한식"},
                                                {"content": "중식"},
                                                {"content": "일식"}
                                              ],
                                              "endTime": "2025-12-31T12:00:00",
                                              "allowRevote": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody VotePostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long votesboardId = votePostService.createVotePost(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new VotePostIdResponse(votesboardId));
    }

    @GetMapping("/{votesboardId}")
    @Operation(
            operationId = "getVotePost",
            summary = "투표 게시글 상세 조회",
            description = "투표 게시글의 상세 정보를 조회합니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotePostDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<VotePostDetailResponse> getVotePost(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long votesboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = userDetails != null ? userDetails.getUser().getId() : null;
        VotePostDetailResponse response = votePostService.getVotePost(votesboardId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            operationId = "getVotePostsByCursor",
            summary = "투표 게시글 목록 조회 (커서 기반)",
            description = "커서 기반 페이지네이션으로 투표 게시글 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotePostListResponse.class)
                    )
            )
    })
    public ResponseEntity<VotePostListResponse> getVotePostList(
            @Parameter(description = "투표 상태 (IN_PROGRESS: 진행중, COMPLETED: 완료, null: 전체)")
            @RequestParam(required = false) VoteStatus status,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "커서 (이전 페이지의 마지막 게시글 ID)")
            @RequestParam(required = false) Long cursor,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = userDetails != null ? userDetails.getUser().getId() : null;
        VotePostListResponse response = votePostService.getVotePostsByCursor(status, size, cursor, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{votesboardId}")
    @Operation(
            operationId = "updateVotePost",
            summary = "투표 게시글 수정",
            description = "투표 게시글을 수정합니다. 투표 옵션은 수정할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 수정 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED_ACCESS\", \"message\": \"접근 권한이 없습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> updateVotePost(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long votesboardId,
            @Valid @RequestBody VotePostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        votePostService.updateVotePost(votesboardId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{votesboardId}")
    @Operation(
            operationId = "deleteVotePost",
            summary = "투표 게시글 삭제",
            description = "투표 게시글을 삭제합니다 (소프트 삭제)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED_ACCESS\", \"message\": \"접근 권한이 없습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> deleteVotePost(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long votesboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        votePostService.deleteVotePost(votesboardId, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{votesboardId}/vote")
    @Operation(
            operationId = "castVote",
            summary = "투표 참여",
            description = """
                    투표에 참여합니다.

                    **제약사항:**
                    - 한 투표당 1번만 참여 가능 (중복 투표 불가)
                    - 진행 중인 투표에만 참여 가능
                    - 선택한 옵션은 해당 투표의 옵션이어야 함

                    **권한:** 로그인 사용자만 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 참여 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "투표 마감",
                                            value = "{\"code\": \"VOTE_CLOSED\", \"message\": \"투표가 마감되었습니다.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "잘못된 옵션",
                                            value = "{\"code\": \"INVALID_VOTE_OPTION\", \"message\": \"유효하지 않은 투표 옵션입니다.\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 투표에 참여함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "중복 투표",
                                    value = "{\"code\": \"ALREADY_VOTED\", \"message\": \"이미 투표에 참여하였습니다.\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 또는 옵션을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "게시글 없음",
                                    value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Void> vote(
            @Parameter(description = "투표 게시글 ID", required = true, example = "1")
            @PathVariable Long votesboardId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "투표 옵션 선택 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VoteRequest.class),
                            examples = @ExampleObject(
                                    name = "투표 참여 예시",
                                    value = "{\"voteOptionId\": 2}"
                            )
                    )
            )
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        votePostService.vote(votesboardId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{votesboardId}/vote")
    @Operation(
            operationId = "changeVote",
            summary = "투표 변경 (재투표)",
            description = """
                    이미 참여한 투표를 변경합니다.

                    **제약사항:**
                    - 재투표가 허용된 경우에만 가능 (allowRevote = true)
                    - 진행 중인 투표에만 가능
                    - 기존에 투표한 기록이 있어야 함

                    **권한:** 로그인 사용자만 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 변경 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "투표 마감",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "투표 마감",
                                    value = "{\"code\": \"VOTE_CLOSED\", \"message\": \"투표가 마감되었습니다.\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "재투표 허용되지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "재투표 불가",
                                    value = "{\"code\": \"REVOTE_NOT_ALLOWED\", \"message\": \"재투표가 허용되지 않습니다.\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "투표 기록을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "투표 기록 없음",
                                    value = "{\"code\": \"VOTE_NOT_FOUND\", \"message\": \"투표 기록을 찾을 수 없습니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Void> changeVote(
            @Parameter(description = "투표 게시글 ID", required = true, example = "1")
            @PathVariable Long votesboardId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "새로 선택할 투표 옵션 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VoteRequest.class),
                            examples = @ExampleObject(
                                    name = "재투표 예시",
                                    value = "{\"voteOptionId\": 3}",
                                    description = "한식(1) → 일식(3)으로 변경"
                            )
                    )
            )
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        votePostService.changeVote(votesboardId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{votesboardId}/vote")
    @Operation(
            operationId = "cancelVote",
            summary = "투표 취소",
            description = "참여한 투표를 취소합니다. 재투표가 허용된 경우에만 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 취소 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "재투표 허용되지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"REVOTE_NOT_ALLOWED\", \"message\": \"재투표가 허용되지 않습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "투표 기록을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"VOTE_NOT_FOUND\", \"message\": \"투표 기록을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> cancelVote(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long votesboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        votePostService.cancelVote(votesboardId, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }
}
