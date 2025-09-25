package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none",
        // JWT 설정 추가
        "jwt.secret-key=ThisIsAVerySecretKeyForTestingPurposesAndItShouldBeLongEnoughToMeetTheRequirements",
        "jwt.access-token-validity-in-ms=3600000",
        "jwt.refresh-token-validity-in-ms=1209600000"
})
@DisplayName("비인증 사용자 접근 테스트")
class UnauthenticatedAccessTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("비인증 상태에서 조회는 성공해야 함")
    void unauthenticatedReadAccess_ShouldSucceed() throws Exception {
        // 먼저 인증된 사용자로 게시글 작성
        Users testUser = Users.builder()
                .nickname("테스터_" + System.currentTimeMillis())
                .email("test" + System.currentTimeMillis() + "@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        Users savedUser = usersRepository.save(testUser);
        usersRepository.flush();

        String accessToken = jwtProvider.generateAccessToken(savedUser.getId());
        String authHeader = "Bearer " + accessToken;

        // 게시글 작성 (인증된 상태)
        MvcResult createResult = mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "공개 테스트 게시글")
                        .param("content", "누구나 볼 수 있는 게시글입니다.")
                        .param("category", "RESTAURANT")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String createResponseContent = createResult.getResponse().getContentAsString();
        FreeboardCreateResponse createResponse = objectMapper.readValue(createResponseContent, FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        // 비인증 상태에서 게시글 목록 조회 (성공해야 함)
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());

        // 비인증 상태에서 게시글 상세 조회 (성공해야 함)
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value("공개 테스트 게시글"));

        // 비인증 상태에서 댓글 목록 조회 (성공해야 함)
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    @DisplayName("비인증 상태에서 작성/수정/삭제는 401 에러")
    void unauthenticatedWriteAccess_ShouldReturn401() throws Exception {
        // 게시글 작성 시도 (401 에러)
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "테스트")
                        .param("content", "테스트")
                        .param("category", "RESTAURANT")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 댓글 작성 시도 (401 에러)
        mockMvc.perform(post("/community/freeboard/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"테스트 댓글\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 게시글 수정 시도 (401 에러)
        mockMvc.perform(multipart("/community/freeboard/1")
                        .param("title", "수정 시도")
                        .param("content", "수정 내용")
                        .param("category", "RESTAURANT")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 게시글 삭제 시도 (401 에러)
        mockMvc.perform(delete("/community/freeboard/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}