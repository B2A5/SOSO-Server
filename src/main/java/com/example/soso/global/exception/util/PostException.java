package com.example.soso.global.exception.util;

import com.example.soso.global.exception.domain.BaseErrorCode;

public class PostException extends BaseException {
    public PostException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
