package com.example.soso.security.config;

import com.example.soso.security.filter.ExceptionHandlerFilter;
import com.example.soso.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JwtSecurityDsl extends AbstractHttpConfigurer<JwtSecurityDsl, HttpSecurity> {

    private boolean enabled;

    @Override
    public void init(HttpSecurity http) throws Exception {
        // Optional: 다른 DSL 추가 시 여기에
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        if (!enabled) return;

        ApplicationContext context = http.getSharedObject(ApplicationContext.class);

        JwtAuthenticationFilter jwtFilter = context.getBean(JwtAuthenticationFilter.class);
        ExceptionHandlerFilter exFilter = context.getBean(ExceptionHandlerFilter.class);

        http.addFilterBefore(exFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }


    public JwtSecurityDsl enable(boolean enable) {
        this.enabled = enable;
        return this;
    }

    public static JwtSecurityDsl customDsl() {
        return new JwtSecurityDsl();
    }
}
