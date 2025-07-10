package com.example.soso.global.exception.util;

import com.example.soso.global.exception.domain.BaseErrorCode;

public class UserAuthException extends BaseException {
    public UserAuthException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
