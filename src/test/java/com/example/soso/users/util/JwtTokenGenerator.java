package com.example.soso.users.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenGenerator {

    private static final String SECRET_KEY = "my-secret-12345678901234567890123456789012";
    private static final long VALIDITY = 1800000; // 30 minutes

    public static void main(String[] args) {
        String userId = "test-user-123";

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + VALIDITY);

        String token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();

        System.out.println("Generated JWT Token for user: " + userId);
        System.out.println(token);
    }
}
