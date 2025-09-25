package com.example.soso.global.exception.util;

import com.example.soso.global.exception.domain.CommentErrorCode;

public class CommentException extends BaseException {

    public CommentException(CommentErrorCode errorCode) {
        super(errorCode);
    }
}