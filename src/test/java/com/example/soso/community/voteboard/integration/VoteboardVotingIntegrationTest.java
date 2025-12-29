package com.example.soso.community.voteboard.integration;

import com.example.soso.community.voteboard.domain.dto.VoteRequest;
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
import java.util.Collections;
import java.util.List;

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

    /**
     * 투표 게시글 생성 헬퍼 메서드
     */
    private Long createVotePost(String title, String content, List<String> options,
                                LocalDateTime endTime, boolean allowRevote, boolean allowMultipleChoice) throws Exception {
        var requestBuilder = multipart("/community/votesboard")
                .param("category", "daily-hobby")
                .param("title", title)
                .param("content", content)
                .param("endTime", endTime.toString())
                .param("allowRevote", String.valueOf(allowRevote))
                .param("allowMultipleChoice", String.valueOf(allowMultipleChoice));

        // 투표 옵션 추가
        for (int i = 0; i < options.size(); i++) {
            requestBuilder.param("voteOptions[" + i + "].content", options.get(i));
        }

        String createResponse = mockMvc.perform(requestBuilder
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createResponse).get("postId").asLong();
    }

    @Test
    @DisplayName("투표 게시글 상세 조회 성공")
    void getVotePost_Success() throws Exception {
        // given - 투표 게시글 생성
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                true,
                false
        );

        // when & then - 상세 조회
        mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(voteboardId))
                .andExpect(jsonPath("$.title").value("점심 메뉴 투표"))
                .andExpect(jsonPath("$.content").value("오늘 점심 뭐 먹을까요?"))
                .andExpect(jsonPath("$.voteOptions").isArray())
                .andExpect(jsonPath("$.voteOptions.length()").value(3))
                .andExpect(jsonPath("$.voteOptions[0].content").value("한식"))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))
                .andExpect(jsonPath("$.voteOptions[0].percentage").value(0.0))
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(0))
                .andExpect(jsonPath("$.voteInfo.voteStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.voteInfo.allowRevote").value(true))
                .andExpect(jsonPath("$.voteInfo.allowMultipleChoice").value(false))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds").isEmpty())
                .andExpect(jsonPath("$.author.nickname").value("투표테스트유저"));
    }

    @Test
    @DisplayName("투표 참여 성공")
    void vote_Success() throws Exception {
        // given - 투표 게시글 생성
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식"),
                LocalDateTime.now().plusDays(7),
                false,
                false
        );

        // 상세 조회로 옵션 ID 가져오기
        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
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
                .voteOptionIds(Collections.singletonList(optionId))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // then - 투표 결과 확인
        mockMvc.perform(get("/community/votesboard/" + voteboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(1))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(1))
                .andExpect(jsonPath("$.voteOptions[0].percentage").value(100.0))
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(0))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[0]").value(optionId));
    }

    @Test
    @DisplayName("투표 참여 실패 - 중복 투표")
    void vote_AlreadyVoted() throws Exception {
        // given - 투표 게시글 생성 및 첫 번째 투표
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식"),
                LocalDateTime.now().plusDays(7),
                false,
                false
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long optionId = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();

        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionIds(Collections.singletonList(optionId))
                .build();

        // 첫 번째 투표
        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // when & then - 두 번째 투표 시도 (중복)
        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("재투표 성공")
    void changeVote_Success() throws Exception {
        // given - 재투표 허용 게시글 생성 및 첫 번째 투표
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                true,  // 재투표 허용
                false
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(1).get("id").asLong();

        // 첫 번째 투표 (한식)
        VoteRequest firstVote = VoteRequest.builder()
                .voteOptionIds(Collections.singletonList(option1Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstVote)))
                .andExpect(status().isOk());

        // when - 재투표 (한식 → 중식)
        VoteRequest changeVote = VoteRequest.builder()
                .voteOptionIds(Collections.singletonList(option2Id))
                .build();

        mockMvc.perform(put("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeVote)))
                .andExpect(status().isOk());

        // then - 변경 결과 확인
        mockMvc.perform(get("/community/votesboard/" + voteboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(1))  // 총 투표수는 그대로
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))  // 한식: 1 → 0
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(1))  // 중식: 0 → 1
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[0]").value(option2Id));
    }

    @Test
    @DisplayName("재투표 실패 - 재투표 허용되지 않음")
    void changeVote_NotAllowed() throws Exception {
        // given - 재투표 불허 게시글 생성 및 투표
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식"),
                LocalDateTime.now().plusDays(7),
                false,  // 재투표 불허
                false
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(1).get("id").asLong();

        // 첫 번째 투표
        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionIds(Collections.singletonList(option1Id)).build())))
                .andExpect(status().isOk());

        // when & then - 재투표 시도 (실패)
        mockMvc.perform(put("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionIds(Collections.singletonList(option2Id)).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("투표 취소 성공")
    void cancelVote_Success() throws Exception {
        // given - 재투표 허용 게시글 생성 및 투표
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식"),
                LocalDateTime.now().plusDays(7),
                true,
                false
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long optionId = objectMapper.readTree(detailResponse)
                .get("voteOptions").get(0).get("id").asLong();

        // 투표
        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VoteRequest.builder().voteOptionIds(Collections.singletonList(optionId)).build())))
                .andExpect(status().isOk());

        // when - 투표 취소
        mockMvc.perform(delete("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk());

        // then - 취소 결과 확인
        mockMvc.perform(get("/community/votesboard/" + voteboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(0))
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds").isEmpty());
    }

    @Test
    @DisplayName("중복 선택 투표 성공 - 2개 옵션 선택")
    void multipleChoiceVote_Success() throws Exception {
        // given - 중복 선택 허용 투표 게시글 생성 (3개 옵션)
        Long voteboardId = createVotePost(
                "좋아하는 음식 선택",
                "좋아하는 음식을 여러 개 선택해주세요",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                false,
                true  // 중복 선택 허용
        );

        // 옵션 ID 가져오기
        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse).get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse).get("voteOptions").get(1).get("id").asLong();

        // when - 2개 옵션 선택 (한식, 중식)
        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionIds(Arrays.asList(option1Id, option2Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        // then - 투표 결과 확인
        mockMvc.perform(get("/community/votesboard/" + voteboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(1))  // 참여자 수는 1명
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(1))  // 한식: 1표
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(1))  // 중식: 1표
                .andExpect(jsonPath("$.voteOptions[2].voteCount").value(0))  // 일식: 0표
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds.length()").value(2))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[0]").value(option1Id))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[1]").value(option2Id))
                .andExpect(jsonPath("$.voteInfo.allowMultipleChoice").value(true));
    }

    @Test
    @DisplayName("중복 선택 투표 실패 - n개 모두 선택 (최대 n-1개)")
    void multipleChoiceVote_TooManyOptions() throws Exception {
        // given - 중복 선택 허용 투표 게시글 생성 (3개 옵션)
        Long voteboardId = createVotePost(
                "좋아하는 음식 선택",
                "좋아하는 음식을 여러 개 선택해주세요",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                false,
                true
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse).get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse).get("voteOptions").get(1).get("id").asLong();
        Long option3Id = objectMapper.readTree(detailResponse).get("voteOptions").get(2).get("id").asLong();

        // when & then - 3개 모두 선택 시도 (실패 - 최대 2개만 가능)
        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionIds(Arrays.asList(option1Id, option2Id, option3Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 선택 투표 실패 - 중복된 옵션 선택")
    void multipleChoiceVote_DuplicateOptions() throws Exception {
        // given - 중복 선택 허용 투표 게시글 생성
        Long voteboardId = createVotePost(
                "좋아하는 음식 선택",
                "좋아하는 음식을 여러 개 선택해주세요",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                false,
                true
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse).get("voteOptions").get(0).get("id").asLong();

        // when & then - 같은 옵션을 중복으로 선택 (실패)
        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionIds(Arrays.asList(option1Id, option1Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단일 선택 투표 실패 - 여러 옵션 선택")
    void singleChoiceVote_MultipleOptionsNotAllowed() throws Exception {
        // given - 단일 선택만 허용하는 투표 게시글 생성
        Long voteboardId = createVotePost(
                "점심 메뉴 투표",
                "오늘 점심 뭐 먹을까요?",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                false,
                false  // 단일 선택만 허용
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse).get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse).get("voteOptions").get(1).get("id").asLong();

        // when & then - 2개 옵션 선택 시도 (실패 - 단일 선택만 가능)
        VoteRequest voteRequest = VoteRequest.builder()
                .voteOptionIds(Arrays.asList(option1Id, option2Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 선택 재투표 성공 - 단일 선택에서 중복 선택으로 변경")
    void changeMultipleChoiceVote_Success() throws Exception {
        // given - 중복 선택 및 재투표 허용 게시글 생성
        Long voteboardId = createVotePost(
                "좋아하는 음식 선택",
                "좋아하는 음식을 여러 개 선택해주세요",
                Arrays.asList("한식", "중식", "일식"),
                LocalDateTime.now().plusDays(7),
                true,  // 재투표 허용
                true
        );

        String detailResponse = mockMvc.perform(get("/community/votesboard/" + voteboardId))
                .andReturn().getResponse().getContentAsString();

        Long option1Id = objectMapper.readTree(detailResponse).get("voteOptions").get(0).get("id").asLong();
        Long option2Id = objectMapper.readTree(detailResponse).get("voteOptions").get(1).get("id").asLong();
        Long option3Id = objectMapper.readTree(detailResponse).get("voteOptions").get(2).get("id").asLong();

        // 첫 번째 투표 - 1개 선택 (한식)
        VoteRequest firstVote = VoteRequest.builder()
                .voteOptionIds(Collections.singletonList(option1Id))
                .build();

        mockMvc.perform(post("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstVote)))
                .andExpect(status().isOk());

        // when - 재투표 (한식 1개 → 중식, 일식 2개)
        VoteRequest changeVote = VoteRequest.builder()
                .voteOptionIds(Arrays.asList(option2Id, option3Id))
                .build();

        mockMvc.perform(put("/community/votesboard/" + voteboardId + "/vote")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeVote)))
                .andExpect(status().isOk());

        // then - 변경 결과 확인
        mockMvc.perform(get("/community/votesboard/" + voteboardId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteInfo.totalVotes").value(1))  // 참여자 수는 그대로
                .andExpect(jsonPath("$.voteOptions[0].voteCount").value(0))  // 한식: 1 → 0
                .andExpect(jsonPath("$.voteOptions[1].voteCount").value(1))  // 중식: 0 → 1
                .andExpect(jsonPath("$.voteOptions[2].voteCount").value(1))  // 일식: 0 → 1
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds.length()").value(2))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[0]").value(option2Id))
                .andExpect(jsonPath("$.voteInfo.selectedOptionIds[1]").value(option3Id));
    }
}
