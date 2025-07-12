package com.example.soso.security.domain;

import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

     private final UsersRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.UNAUTHORIZED_USER));

        return new CustomUserDetails(user);
    }
}
