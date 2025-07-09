package com.example.soso.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private String secretKey;
    private long accessTokenValidityInMs;
    private long refreshTokenValidityInMs;
}
