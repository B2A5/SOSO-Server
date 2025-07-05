package com.example.soso.kakao.dto;

import java.util.HashMap;
import java.util.Map;

public record KakaoTokenRequest(
        String grant_type,
        String client_id,
        String redirect_uri,
        String code,
        String code_verifier
) {
    public static KakaoTokenRequest of(String clientId, String redirectUri, String code, String verifier) {
        return new KakaoTokenRequest(
                "authorization_code",
                clientId,
                redirectUri,
                code,
                verifier
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("grant_type", grant_type);
        map.put("client_id", client_id);
        map.put("redirect_uri", redirect_uri);
        map.put("code", code);
        map.put("code_verifier", code_verifier);
        return map;
    }
}

