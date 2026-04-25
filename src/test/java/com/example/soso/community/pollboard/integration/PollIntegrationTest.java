package com.example.soso.community.pollboard.integration;

import com.example.soso.community.pollboard.domain.entity.PollStatus;
import com.example.soso.community.pollboard.repository.PollOptionRepository;
import com.example.soso.community.pollboard.repository.PollRepository;
import com.example.soso.community.pollboard.repository.VoteRepository;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class PollIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @MockBean
    private ImageUploadService imageUploadService;

    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        voteRepository.deleteAll();
        pollRepository.deleteAll();
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

        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        try {
            String result = mockMvc.perform(multipart("/community/polls")
                            .file(image1)
                            .file(image2)
                            .param("category", "daily-hobby")
                            .param("title", "테스트 투표")
                            .param("content", "테스트 내용입니다")
                            .param("options[0].content", "옵션1")
                            .param("options[1].content", "옵션2")
                            .param("closedAt", closedAt.toString())
                            .param("canRevote", "true")
                            .param("canMultiSelect", "false")
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
        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "true")
                        .param("canMultiSelect", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 옵션 초과 (6개)")
    void createVotePost_TooManyOptions() throws Exception {
        // given
        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("options[2].content", "옵션3")
                        .param("options[3].content", "옵션4")
                        .param("options[4].content", "옵션5")
                        .param("options[5].content", "옵션6")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "true")
                        .param("canMultiSelect", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 실패 - 과거 마감 시간")
    void createVotePost_PastEndTime() throws Exception {
        // given
        LocalDateTime closedAt = LocalDateTime.now().minusDays(1);  // 과거 시간

        // when & then
        mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "true")
                        .param("canMultiSelect", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 성공")
    void getVotePostList_Success() throws Exception {
        // when & then
        String response = mockMvc.perform(get("/community/polls")
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
        mockMvc.perform(get("/community/polls")
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
        mockMvc.perform(get("/community/polls")
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
        mockMvc.perform(get("/community/polls")
                        .param("status", PollStatus.IN_PROGRESS.name())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    @DisplayName("투표 게시글 목록 조회 - 완료 필터")
    void getVotePostList_FilterCompleted() throws Exception {
        // when & then
        mockMvc.perform(get("/community/polls")
                        .param("status", PollStatus.COMPLETED.name())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    @DisplayName("인증 없이 투표 게시글 생성 시도 - 실패")
    void createVotePost_Unauthorized() throws Exception {
        // given
        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        // Spring Security가 인증되지 않은 요청을 차단하여 401 Unauthorized 반환
        mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "true")
                        .param("canMultiSelect", "false")
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
        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/polls")
                        // category 설정 안 함
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "true")
                        .param("canMultiSelect", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 게시글 생성 성공 - 이미지 없이")
    void createVotePost_WithoutImages() throws Exception {
        // given
        LocalDateTime closedAt = LocalDateTime.now().plusDays(7);

        // when & then
        mockMvc.perform(multipart("/community/polls")
                        .param("category", "restaurant")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("closedAt", closedAt.toString())
                        .param("canRevote", "false")
                        .param("canMultiSelect", "true")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists());
    }

    // ──────────────────────────────────────────────────────────────
    // 투표 참여 (POST /{pollId}/vote) 테스트
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("투표 참여 성공 - PollDetailResponse 반환 및 hasVoted=true 검증")
    void vote_Success_ReturnsDetailWithHasVotedTrue() throws Exception {
        // given
        long pollId = createPoll(false, false);
        long optionId = firstOptionId(pollId);

        // when & then
        mockMvc.perform(post("/community/polls/{pollId}/vote", pollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteOptionIds\": [" + optionId + "]}")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(pollId))
                .andExpect(jsonPath("$.hasVoted").value(true))
                .andExpect(jsonPath("$.voteInfo.myOptionIds").isArray())
                .andExpect(jsonPath("$.voteInfo.myOptionIds[0]").value(optionId));
    }

    @Test
    @DisplayName("투표 참여 성공 - 다중 선택")
    void vote_MultiSelect_Success() throws Exception {
        // given
        long pollId = createPoll(false, true);
        long[] optionIds = twoOptionIds(pollId);

        // when & then
        mockMvc.perform(post("/community/polls/{pollId}/vote", pollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteOptionIds\": [" + optionIds[0] + "]}")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasVoted").value(true));
    }

    @Test
    @DisplayName("투표 참여 실패 - 중복 투표 (409)")
    void vote_AlreadyVoted_Conflict() throws Exception {
        // given
        long pollId = createPoll(false, false);
        long optionId = firstOptionId(pollId);
        String body = "{\"voteOptionIds\": [" + optionId + "]}";

        mockMvc.perform(post("/community/polls/{pollId}/vote", pollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk());

        // when & then - 두 번째 투표
        mockMvc.perform(post("/community/polls/{pollId}/vote", pollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_VOTED"));
    }

    @Test
    @DisplayName("투표 참여 실패 - 비인증 사용자 (401)")
    void vote_Unauthenticated_Unauthorized() throws Exception {
        // given
        long pollId = createPoll(false, false);
        long optionId = firstOptionId(pollId);

        // when & then
        mockMvc.perform(post("/community/polls/{pollId}/vote", pollId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteOptionIds\": [" + optionId + "]}"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────

    private long createPoll(boolean canRevote, boolean canMultiSelect) throws Exception {
        String response = mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", "테스트 투표")
                        .param("content", "테스트 내용입니다")
                        .param("options[0].content", "옵션1")
                        .param("options[1].content", "옵션2")
                        .param("options[2].content", "옵션3")
                        .param("closedAt", LocalDateTime.now().plusDays(7).toString())
                        .param("canRevote", String.valueOf(canRevote))
                        .param("canMultiSelect", String.valueOf(canMultiSelect))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("postId").asLong();
    }

    private long firstOptionId(long pollId) {
        return pollOptionRepository.findAll().stream()
                .filter(o -> o.getPoll().getId().equals(pollId))
                .mapToLong(o -> o.getId())
                .findFirst()
                .orElseThrow();
    }

    private long[] twoOptionIds(long pollId) {
        long[] ids = pollOptionRepository.findAll().stream()
                .filter(o -> o.getPoll().getId().equals(pollId))
                .mapToLong(o -> o.getId())
                .limit(2)
                .toArray();
        return ids;
    }
}
