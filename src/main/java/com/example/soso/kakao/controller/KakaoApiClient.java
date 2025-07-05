package com.example.soso.kakao.controller;

import com.example.soso.global.config.FeignConfig;
import com.example.soso.kakao.dto.KakaoUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "KakaoApiClient",
        url = "https://kapi.kakao.com",
        configuration = FeignConfig.class
)
public interface KakaoApiClient {

    @GetMapping("/v2/user/me")
    KakaoUserResponse getUserInfo(@RequestHeader("Authorization") String accessToken);
}
