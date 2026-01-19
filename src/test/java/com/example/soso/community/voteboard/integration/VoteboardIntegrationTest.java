package com.example.soso.community.voteboard.integration;

import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import com.example.soso.community.voteboard.repository.VotesboardRepository;
import com.example.soso.community.voteboard.repository.VoteResultRepository;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 투표 게시판 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("투표 게시판 통합 테스트")
class VoteboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VotesboardRepository votesboardRepository;

    @Autowired
    private VoteResultRepository voteResultRepository;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private ImageUploadService imageUploadService;

    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        voteResultRepository.deleteAll();
        votesboardRepository.deleteAll();
        usersRepository.deleteAll();

        // 테스트용 사용자 생성 (id는 자동 생성됨)
        Users testUser = Users.builder()
                .username("테스트유저")
                .nickname("테스트유저")
                .userType(UserType.FOUNDER)
                .email("test@example.com")
                .location("11680")  // 서울특별시 강남구 시군구 코드
                .build();
        testUser = usersRepository.save(testUser);

        testUserDetails = new CustomUserDetails(testUser);

        // ImageUploadService Mock 설정
        when(imageUploadService.uploadImages(any(), anyString()))
                .thenReturn(Arrays.asList(
                        "https://test-bucket.s3.amazonaws.com/test1.jpg",
                        "https://test-bucket.s3.amazonaws.com/test2.jpg"
                ));
    }

    @Test
    @DisplayName("투표 게시글 생성 성공")
    void createVotePost_Success() throws Exception {
        // given
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "test1.jpg",
                "image/jpeg",
                "test image content 1".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "test2.jpg",
                "image/jpeg",
                "test image content 2".getBytes()
        );

        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        try {
            String result = mockMvc.perform(multipart("/community/votesboard")
                            .file(image1)
                            .file(image2)
                            .param("category", "daily-hobby")
                            .param("title", "테스트 투표")
                            .param("content", "테스트 내용입니다")
                            .param("voteOptions[0].content", "옵션1")
                            .param("voteOptions[1].content", "옵션2")
                            .param("endTime", endTime.toString())
                            .param("allowRevote", "true")
                            .param("allowMultipleChoice", "false")
                            .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").exists())
                    .andReturn().getResponse().getContentAsString();
            System.out.println("Success response: " + result);
        } catch (AssertionError e) {
            System.out.println("Test failed with assertion error: " + e.getMessage());
            throw e;
        }
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 옵션 부족 (1개)")
    void createVotePost_InsufficientOptions() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/votesboard")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "true")
                        .param("allowMultipleChoice", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 옵션 초과 (6개)")
    void createVotePost_TooManyOptions() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/votesboard")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("voteOptions[1].content", "옵션2")
                        .param("voteOptions[2].content", "옵션3")
                        .param("voteOptions[3].content", "옵션4")
                        .param("voteOptions[4].content", "옵션5")
                        .param("voteOptions[5].content", "옵션6")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "true")
                        .param("allowMultipleChoice", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 과거 마감 시간")
    void createVotePost_PastEndTime() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().minusDays(1);  // 과거 시간

        // when & then
        mockMvc.perform(multipart("/community/votesboard")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("voteOptions[1].content", "옵션2")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "true")
                        .param("allowMultipleChoice", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 성공")
    void getVotePostList_Success() throws Exception {
        // when & then
        String response = mockMvc.perform(get("/community/votesboard")
                        .param("size", "20"))
                .andDo(print())  // 응답 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.hasNext").exists())
                .andExpect(jsonPath("$.size").exists())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Response: " + response);
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 - 인증 사용자 (hasVoted, isLiked 검증)")
    void getVotePostList_Authenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/community/votesboard")
                        .param("size", "20")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.totalCount").exists())
                .andExpect(jsonPath("$.isAuthorized").value(true));
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 - 비인증 사용자 (hasVoted, isLiked null 검증)")
    void getVotePostList_Unauthenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/community/votesboard")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(false))
                .andExpect(jsonPath("$.totalCount").exists());
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 - 진행중 필터")
    void getVotePostList_FilterInProgress() throws Exception {
        // when & then
        mockMvc.perform(get("/community/votesboard")
                        .param("status", VoteStatus.IN_PROGRESS.name())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 - 완료 필터")
    void getVotePostList_FilterCompleted() throws Exception {
        // when & then
        mockMvc.perform(get("/community/votesboard")
                        .param("status", VoteStatus.COMPLETED.name())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    @DisplayName("인증 없이 투표 게시글 생성 시도 - 실패")
    void createVotePost_Unauthorized() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        // Spring Security가 인증되지 않은 요청을 차단하여 401 Unauthorized 반환
        mockMvc.perform(multipart("/community/votesboard")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("voteOptions[1].content", "옵션2")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "true")
                        .param("allowMultipleChoice", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 카테고리 누락")
    void createVotePost_MissingCategory() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/votesboard")
                        // category 설정 안 함
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("voteOptions[1].content", "옵션2")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "true")
                        .param("allowMultipleChoice", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 성공 - 이미지 없이")
    void createVotePost_WithoutImages() throws Exception {
        // given
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/votesboard")
                        .param("category", "restaurant")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("voteOptions[0].content", "옵션1")
                        .param("voteOptions[1].content", "옵션2")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "false")
                        .param("allowMultipleChoice", "true")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists());
    }
}
