package com.example.soso.global.exception.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PostErrorCode implements BaseErrorCode {

    POST_NOT_FOUND("해당 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("게시글에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("해당 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FORBIDDEN("게시글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_CATEGORY("유효하지 않은 카테고리입니다.", HttpStatus.BAD_REQUEST),
    EMPTY_CONTENT("게시글 내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    LIKE_COUNT_NOT_FOUND("좋아요 수를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("없는 댓글 입니다", HttpStatus.NOT_FOUND),
    COMMENT_ALREADY_DELETED("이미 삭제된 댓글입니다.", HttpStatus.BAD_REQUEST),
    REPLY_DEPTH_EXCEEDED("대댓글의 대댓글은 허용되지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_CURSOR("유효하지 않은 커서 값입니다.", HttpStatus.BAD_REQUEST),

    // 투표 관련 에러
    VOTE_CLOSED("투표가 마감되었습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_VOTED("이미 투표에 참여하였습니다.", HttpStatus.CONFLICT),
    INVALID_VOTE_OPTION("유효하지 않은 투표 옵션입니다.", HttpStatus.BAD_REQUEST),
    REVOTE_NOT_ALLOWED("재투표가 허용되지 않습니다.", HttpStatus.FORBIDDEN),
    VOTE_NOT_FOUND("투표 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    VOTE_OPTION_NOT_FOUND("투표 옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
