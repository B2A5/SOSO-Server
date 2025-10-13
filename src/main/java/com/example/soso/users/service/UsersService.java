package com.example.soso.users.service;

import com.example.soso.users.domain.dto.UserResponse;

public interface UsersService {

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    UserResponse getCurrentUserInfo(String userId);

}
