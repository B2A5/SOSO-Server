package com.example.soso.community.voteboard.integration;

import com.example.soso.community.voteboard.domain.dto.*;
import com.example.soso.community.voteboard.repository.VotePostRepository;
import com.example.soso.community.voteboard.repository.VoteResultRepository;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.domain.entity.UserType;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 투표 참여/재투표/취소 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("투표 참여 통합 테스트")
class VoteboardVotingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    private Users testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // 고유한 이메일로 사용자 생성 (다른 테스트와 충돌 방지)
        testUser = Users.builder()
                .username("투표테스트유저_" + System.currentTimeMillis())
                .nickname("투표테스트유저")
                .userType(UserType.FOUNDER)
                .email("votetest_" + System.currentTimeMillis() + "@example.com")
                .location("11680")  // 서울특별시 강남구 시군구 코드
                .build();
        testUser = usersRepository.save(testUser);

        testUserDetails = new CustomUserDetails(testUser);
    }

    @Test
    @DisplayName("투표 게시글 상세 조회 성공")
    void getVotePost_Success() throws Exception {
        // given - 투표 게시글 생성
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build(),
                        VoteOptionRequest.builder().content("일식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(true)
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        // when & then - 상세 조회
        mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(votesboardId))
                .andExpect(jsonPath("$.title").value("점심 메뉴 투표"))
                .andExpect(jsonPath("$.content").value("오늘 점심 뭐 먹을까요?"))
                .andExpect(jsonPath("$.voteOptions").isArray())
                .andExpect(jsonPath("$.voteOptions.length()").value(3))
                .andExpect(jsonPath("$.voteOptions[0].content").value("한식"))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))
                .andExpect(jsonPath("$.voteOptions[0].percentage").value(0.0))
                .andExpect(jsonPath("$.totalVotes").value(0))
                .andExpect(jsonPath("$.voteStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.allowRevote").value(true))
                .andExpect(jsonPath("$.selectedOptionId").doesNotExist())
                .andExpect(jsonPath("$.author.nickname").value("투표테스트유저"));
    }

    @Test
    @DisplayName("투표 참여 성공")
    void vote_Success() throws Exception {
        // given - 투표 게시글 생성
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(false)
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        // 상세 조회로 옵션 ID 가져오기
        String detailResponse = mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long optionId = objectMapper.readTree(detailResponse)
                .get("voteOptions")
                .get(0)
                .get("id")
                .asLong();

        // when - 투표 참여
        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionId(optionId)
                .build();

        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // then - 투표 결과 확인
        mockMvc.perform(get("/community/votesboard/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(1))
                .andExpect(jsonPath("$.voteOptions[0].percentage").value(100.0))
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(0))
                .andExpect(jsonPath("$.selectedOptionId").value(optionId));
    }

    @Test
    @DisplayName("투표 참여 실패 - 중복 투표")
    void vote_AlreadyVoted() throws Exception {
        // given - 투표 게시글 생성 및 첫 번째 투표
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(false)
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andReturn().getResponse().getContentAsString();

        Long optionId = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();

        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionId(optionId)
                .build();

        // 첫 번째 투표
        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // when & then - 두 번째 투표 시도 (중복)
        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("재투표 성공")
    void changeVote_Success() throws Exception {
        // given - 재투표 허용 게시글 생성 및 첫 번째 투표
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build(),
                        VoteOptionRequest.builder().content("일식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(true)  // 재투표 허용
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(1).get("id").asLong();

        // 첫 번째 투표 (한식)
        VoteRequest firstVote = VoteRequest.builder()
                .voteOptionId(option1Id)
                .build();

        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstVote)))
                .andExpect(status().isOk());

        // when - 재투표 (한식 → 중식)
        VoteRequest changeVote = VoteRequest.builder()
                .voteOptionId(option2Id)
                .build();

        mockMvc.perform(put("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeVote)))
                .andExpect(status().isOk());

        // then - 변경 결과 확인
        mockMvc.perform(get("/community/votesboard/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1))  // 총 투표수는 그대로
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))  // 한식: 1 → 0
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(1))  // 중식: 0 → 1
                .andExpect(jsonPath("$.selectedOptionId").value(option2Id));
    }

    @Test
    @DisplayName("재투표 실패 - 재투표 허용되지 않음")
    void changeVote_NotAllowed() throws Exception {
        // given - 재투표 불허 게시글 생성 및 투표
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(false)  // 재투표 불허
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(1).get("id").asLong();

        // 첫 번째 투표
        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionId(option1Id).build())))
                .andExpect(status().isOk());

        // when & then - 재투표 시도 (실패)
        mockMvc.perform(put("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionId(option2Id).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("투표 취소 성공")
    void cancelVote_Success() throws Exception {
        // given - 재투표 허용 게시글 생성 및 투표
        VotePostCreateRequest createRequest = VotePostCreateRequest.builder()
                .title("점심 메뉴 투표")
                .content("오늘 점심 뭐 먹을까요?")
                .voteOptions(Arrays.asList(
                        VoteOptionRequest.builder().content("한식").build(),
                        VoteOptionRequest.builder().content("중식").build()
                ))
                .endTime(LocalDateTime.now().plusDays(7))
                .allowRevote(true)
                .build();

        String createResponse = mockMvc.perform(post("/community/votesboard")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long votesboardId = objectMapper.readTree(createResponse).get("votesboardId").asLong();

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + votesboardId))
                .andReturn().getResponse().getContentAsString();

        Long optionId = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();

        // 투표
        mockMvc.perform(post("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionId(optionId).build())))
                .andExpect(status().isOk());

        // when - 투표 취소
        mockMvc.perform(delete("/community/votesboard/" + votesboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk());

        // then - 취소 결과 확인
        mockMvc.perform(get("/community/votesboard/" + votesboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(0))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))
                .andExpect(jsonPath("$.selectedOptionId").doesNotExist());
    }
}
