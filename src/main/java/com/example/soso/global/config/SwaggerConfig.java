package com.example.soso.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String SECURITY_SCHEME_NAME = "Authorization";

        return new OpenAPI()
                .info(new Info()
                        .title("SoSo API 명세서")
                        .description("소소한 아이디어 공유 플랫폼의 백엔드 API 문서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME) // 헤더 이름 → Authorization
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer customOpenAPI() {
        List<String> tagOrder = List.of(
                "Signup",
                "Auth",
                "Freeboard",
                "Freeboard Like",
                "Freeboard Comment",
                "Freeboard Comment Like",
                "Votesboard",
                "Votesboard Like",
                "Votesboard Comment",
                "Votesboard Comment Like"
        );

        return openApi -> openApi.setTags(
                openApi.getTags().stream()
                        .sorted(Comparator.comparingInt(tag -> {
                            String tagName = tag.getName();
                            for (int i = 0; i < tagOrder.size(); i++) {
                                if (tagName.equals(tagOrder.get(i))) {
                                    return i;
                                }
                            }
                            return tagOrder.size();
                        }))
                        .toList()
        );
    }
}


