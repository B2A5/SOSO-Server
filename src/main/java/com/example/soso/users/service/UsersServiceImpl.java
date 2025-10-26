package com.example.soso.users.service;

import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUserInfo(String userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

}
