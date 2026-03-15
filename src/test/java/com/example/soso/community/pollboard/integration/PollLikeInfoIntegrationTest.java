package com.example.soso.community.pollboard.integration;

import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 투표 게시판 좋아요 정보 통합 테스트 (TDD)
 *
 * 테스트 시나리오:
 * 1. 게시글 상세 조회 시 likeCount와 isLiked 포함 확인
 * 2. 게시글 목록 조회 시 likeCount와 isLiked 포함 확인
 * 3. 좋아요 추가 후 상세 조회 시 likeCount 증가 및 isLiked = true 확인
 * 4. 좋아요 취소 후 상세 조회 시 likeCount 감소 및 isLiked = false 확인
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("투표 게시판 좋아요 정보 통합 테스트")
class PollLikeInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    private Users testUser;
    private CustomUserDetails testUserDetails;
    private Users anotherUser;
    private CustomUserDetails anotherUserDetails;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        testUser = Users.builder()
                .username("좋아요테스트유저1_" + System.currentTimeMillis())
                .nickname("좋아요테스트1")
                .userType(UserType.FOUNDER)
                .email("likeinfo1_" + System.currentTimeMillis() + "@example.com")
                .location("11680")
                .build();
        testUser = usersRepository.save(testUser);
        testUserDetails = new CustomUserDetails(testUser);

        // 다른 유저 생성
        anotherUser = Users.builder()
                .username("좋아요테스트유저2_" + System.currentTimeMillis())
                .nickname("좋아요테스트2")
                .userType(UserType.INHABITANT)
                .email("likeinfo2_" + System.currentTimeMillis() + "@example.com")
                .location("11680")
                .build();
        anotherUser = usersRepository.save(anotherUser);
        anotherUserDetails = new CustomUserDetails(anotherUser);
    }

    @Test
    @DisplayName("게시글 상세 조회 시 좋아요 정보 포함 - 좋아요 없는 상태")
    void getVotesboardDetail_WithLikeInfo_NoLikes() throws Exception {
        // given - 투표 게시글 생성
        Long votesboardId = createVotesboard(testUserDetails, "좋아요 정보 테스트");

        // when & then - 상세 조회
        mockMvc.perform(get("/community/polls/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(votesboardId))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.isLiked").value(false));
    }

    @Test
    @DisplayName("게시글 상세 조회 시 좋아요 정보 포함 - 좋아요 있는 상태")
    void getVotesboardDetail_WithLikeInfo_WithLikes() throws Exception {
        // given - 투표 게시글 생성 및 좋아요 추가
        Long votesboardId = createVotesboard(testUserDetails, "좋아요 정보 테스트2");

        // 다른 유저가 좋아요
        mockMvc.perform(post("/community/polls/" + votesboardId + "/like")
                        .with(SecurityMockMvcRequestPostProcessors.user(anotherUserDetails)))
                .andExpect(status().isOk());

        // 현재 유저도 좋아요
        mockMvc.perform(post("/community/polls/" + votesboardId + "/like")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk());

        // when & then - 상세 조회 (현재 유저가 좋아요한 상태)
        mockMvc.perform(get("/community/polls/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(votesboardId))
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.isLiked").value(true));

        // 다른 유저 관점에서 조회
        mockMvc.perform(get("/community/polls/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(anotherUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.isLiked").value(true));
    }

    @Test
    @DisplayName("게시글 목록 조회 시 좋아요 정보 포함")
    void getVotesboardList_WithLikeInfo() throws Exception {
        // given - 여러 투표 게시글 생성
        Long votesboardId1 = createVotesboard(testUserDetails, "투표1");
        Long votesboardId2 = createVotesboard(testUserDetails, "투표2");
        Long votesboardId3 = createVotesboard(testUserDetails, "투표3");

        // 첫 번째 게시글에 좋아요 2개
        mockMvc.perform(post("/community/polls/" + votesboardId1 + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)));
        mockMvc.perform(post("/community/polls/" + votesboardId1 + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(anotherUserDetails)));

        // 두 번째 게시글에 좋아요 1개 (현재 유저만)
        mockMvc.perform(post("/community/polls/" + votesboardId2 + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)));

        // when & then - 목록 조회 (현재 유저 관점)
        mockMvc.perform(get("/community/polls")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].likeCount").exists())
                .andExpect(jsonPath("$.posts[0].isLiked").exists())
                // 최신순이므로 votesboardId3이 첫 번째
                .andExpect(jsonPath("$.posts[0].postId").value(votesboardId3))
                .andExpect(jsonPath("$.posts[0].likeCount").value(0))
                .andExpect(jsonPath("$.posts[0].isLiked").value(false))
                // 두 번째는 votesboardId2
                .andExpect(jsonPath("$.posts[1].postId").value(votesboardId2))
                .andExpect(jsonPath("$.posts[1].likeCount").value(1))
                .andExpect(jsonPath("$.posts[1].isLiked").value(true))
                // 세 번째는 votesboardId1
                .andExpect(jsonPath("$.posts[2].postId").value(votesboardId1))
                .andExpect(jsonPath("$.posts[2].likeCount").value(2))
                .andExpect(jsonPath("$.posts[2].isLiked").value(true));
    }

    @Test
    @DisplayName("비로그인 사용자의 게시글 상세 조회 - isLiked는 null")
    void getVotesboardDetail_WithoutAuth_IsLikedAlwaysFalse() throws Exception {
        // given - 투표 게시글 생성 및 좋아요 추가
        Long votesboardId = createVotesboard(testUserDetails, "비로그인 테스트");

        mockMvc.perform(post("/community/polls/" + votesboardId + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)));

        // when & then - 비로그인 상태에서 조회
        mockMvc.perform(get("/community/polls/" + votesboardId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.isLiked").doesNotExist()); // 비인증 사용자는 null (JSON에서는 필드가 포함되지 않음)
    }

    @Test
    @DisplayName("좋아요 토글 후 상세 조회 - likeCount와 isLiked 변경 확인")
    void toggleLike_AndCheckDetailResponse() throws Exception {
        // given - 투표 게시글 생성
        Long votesboardId = createVotesboard(testUserDetails, "토글 테스트");

        // when - 좋아요 추가
        mockMvc.perform(post("/community/polls/" + votesboardId + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)));

        // then - 상세 조회에서 확인
        mockMvc.perform(get("/community/polls/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.isLiked").value(true));

        // when - 좋아요 취소
        mockMvc.perform(post("/community/polls/" + votesboardId + "/like")
                .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)));

        // then - 상세 조회에서 확인
        mockMvc.perform(get("/community/polls/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.isLiked").value(false));
    }

    /**
     * 투표 게시글 생성 헬퍼 메서드
     */
    private Long createVotesboard(CustomUserDetails userDetails, String title) throws Exception {
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        MvcResult result = mockMvc.perform(multipart("/community/polls")
                        .param("category", "daily-hobby")
                        .param("title", title)
                        .param("content", "테스트 내용")
                        .param("voteOptions[0].content", "옵션 A")
                        .param("voteOptions[1].content", "옵션 B")
                        .param("endTime", endTime.toString())
                        .param("allowRevote", "false")
                        .param("allowMultipleChoice", "false")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("postId").asLong();
    }
}
