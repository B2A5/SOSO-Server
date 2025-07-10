package com.example.soso.global.exception.util;

import com.example.soso.global.exception.domain.BaseErrorCode;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
