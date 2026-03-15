package com.example.soso.community.common.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 커서 기반 페이지네이션 공통 메타 정보
 *
 * <p>자유게시판(PostCursorResponse)과 투표게시판(PollCursorResponse) 모두
 * 이 레코드를 포함하여 페이지네이션 메타를 표현한다.</p>
 */
@Schema(description = "커서 페이지네이션 메타 정보")
public record BoardCursorMeta(

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,

        @Schema(description = "다음 페이지 요청에 사용할 커서 값 (마지막 페이지면 null)")
        String nextCursor,

        @Schema(description = "현재 페이지의 실제 항목 수")
        int size,

        @Schema(description = "조건에 맞는 전체 항목 수")
        long totalCount,

        @Schema(description = "현재 요청이 인증된 사용자인지 여부")
        boolean authorized
) {
}
