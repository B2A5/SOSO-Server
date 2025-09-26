package com.example.soso.global.exception.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CommentErrorCode implements BaseErrorCode {

    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("댓글에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    COMMENT_POST_MISMATCH("댓글이 해당 게시글에 속하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PARENT_COMMENT("유효하지 않은 부모 댓글입니다.", HttpStatus.BAD_REQUEST),
    REPLY_DEPTH_EXCEEDED("대댓글의 대댓글은 허용되지 않습니다.", HttpStatus.BAD_REQUEST),
    DELETED_COMMENT_CANNOT_BE_MODIFIED("삭제된 댓글은 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMENT_CONTENT_EMPTY("댓글 내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMENT_CONTENT_TOO_LONG("댓글이 너무 깁니다.", HttpStatus.BAD_REQUEST);

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