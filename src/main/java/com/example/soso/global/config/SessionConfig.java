package com.example.soso.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?(\\w+\\.[a-z]+)$");
        serializer.setUseBase64Encoding(false);
        serializer.setSameSite("None");  // 크로스 사이트 쿠키 허용 (localhost → soso.dreampaste.com)
        serializer.setUseSecureCookie(true);  // HTTPS 필수 (SameSite=None 요구사항)
        return serializer;
    }
}