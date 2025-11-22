package com.example.soso.community.voteboard.integration;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.voteboard.comment.domain.dto.VoteboardCommentCreateRequest;
import com.example.soso.community.voteboard.domain.dto.VoteOptionRequest;
import com.example.soso.community.voteboard.domain.dto.VotePostCreateRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 투표 게시판 댓글 시스템 통합 테스트 (TDD)
 *
 * 테스트 시나리오:
 * 1. 댓글 작성 - 일반 댓글
 * 2. 댓글 목록 조회
 * 3. 댓글 수정
 * 4. 댓글 삭제
 * 5. 대댓글 작성
 * 6. 댓글 좋아요 토글
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("투표 게시판 댓글 시스템 통합 테스트")
class VoteboardCommentIntegrationTest {

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
                .username("댓글테스트유저1_" + System.currentTimeMillis())
                .nickname("댓글테스트1")
                .userType(UserType.FOUNDER)
                .email("comment1_" + System.currentTimeMillis() + "@example.com")
                .location("11680")
                .build();
        testUser = usersRepository.save(testUser);
        testUserDetails = new CustomUserDetails(testUser);

        // 다른 유저 생성
        anotherUser = Users.builder()
                .username("댓글테스트유저2_" + System.currentTimeMillis())
                .nickname("댓글테스트2")
                .userType(UserType.INHABITANT)
                .email("comment2_" + System.currentTimeMillis() + "@example.com")
                .location("11680")
                .build();
        anotherUser = usersRepository.save(anotherUser);
        anotherUserDetails = new CustomUserDetails(anotherUser);
    }

    @Test
    @DisplayName("댓글 작성 - 일반 댓글")
    void createComment_Success() throws Exception {
        // given - 투표 게시글 생성
        Long votePostId = createVotePost(testUserDetails, "댓글 테스트 게시글");

        VoteboardCommentCreateRequest request = VoteboardCommentCreateRequest.builder()
                .content("첫 번째 댓글입니다")
                .build();

        // when & then
        mockMvc.perform(post("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists());
    }

    @Test
    @DisplayName("댓글 목록 조회 - 댓글 0개")
    void getComments_Empty() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "댓글 없는 게시글");

        // when & then
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(0))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("댓글 목록 조회 - 댓글 2개")
    void getComments_WithMultipleComments() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "댓글 테스트 게시글");
        Long comment1Id = createComment(votePostId, testUserDetails, "첫 번째 댓글");
        Long comment2Id = createComment(votePostId, anotherUserDetails, "두 번째 댓글");

        // when & then
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(comment2Id))
                .andExpect(jsonPath("$.comments[1].commentId").value(comment1Id))
                .andExpect(jsonPath("$.comments[0].content").value("두 번째 댓글"))
                .andExpect(jsonPath("$.comments[1].content").value("첫 번째 댓글"));
    }

    @Test
    @DisplayName("댓글 수정 - 본인 댓글")
    void updateComment_AsAuthor_Success() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "댓글 수정 테스트");
        Long commentId = createComment(votePostId, testUserDetails, "원래 댓글");

        VoteboardCommentCreateRequest updateRequest = VoteboardCommentCreateRequest.builder()
                .content("수정된 댓글 내용")
                .build();

        // when & then
        mockMvc.perform(patch("/community/votesboard/" + votePostId + "/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId));

        // 수정 확인
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].content").value("수정된 댓글 내용"));
    }

    @Test
    @DisplayName("댓글 삭제 - 본인 댓글")
    void deleteComment_AsAuthor_Success() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "댓글 삭제 테스트");
        Long commentId = createComment(votePostId, testUserDetails, "삭제될 댓글");

        // when - 댓글 삭제
        mockMvc.perform(delete("/community/votesboard/" + votePostId + "/comments/" + commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isNoContent());

        // then - 삭제된 댓글은 "삭제된 댓글입니다" 표시
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].deleted").value(true))
                .andExpect(jsonPath("$.comments[0].content").value("삭제된 댓글입니다"));
    }

    @Test
    @DisplayName("대댓글 작성")
    void createReply_Success() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "대댓글 테스트");
        Long parentCommentId = createComment(votePostId, testUserDetails, "부모 댓글");

        VoteboardCommentCreateRequest replyRequest = VoteboardCommentCreateRequest.builder()
                .content("대댓글입니다")
                .parentCommentId(parentCommentId)
                .build();

        // when & then
        mockMvc.perform(post("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(anotherUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists());

        // 댓글 목록 조회 시 대댓글 확인
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].parentCommentId").value(parentCommentId))
                .andExpect(jsonPath("$.comments[0].content").value("대댓글입니다"))
                .andExpect(jsonPath("$.comments[1].replyCount").value(1));
    }

    @Test
    @DisplayName("댓글 좋아요 토글")
    void toggleCommentLike_Success() throws Exception {
        // given
        Long votePostId = createVotePost(testUserDetails, "댓글 좋아요 테스트");
        Long commentId = createComment(votePostId, anotherUserDetails, "좋아요 테스트 댓글");

        // when - 좋아요 추가
        mockMvc.perform(post("/community/votesboard/" + votePostId + "/comments/" + commentId + "/like")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        // then - 댓글 목록에서 좋아요 확인
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].likeCount").value(1))
                .andExpect(jsonPath("$.comments[0].isLiked").value(true));

        // when - 좋아요 취소
        mockMvc.perform(post("/community/votesboard/" + votePostId + "/comments/" + commentId + "/like")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

        // then - 댓글 목록에서 좋아요 취소 확인
        mockMvc.perform(get("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].likeCount").value(0))
                .andExpect(jsonPath("$.comments[0].isLiked").value(false));
    }

    // Helper methods

    private Long createVotePost(CustomUserDetails userDetails, String title) throws Exception {
        VotePostCreateRequest request = new VotePostCreateRequest();
        request.setCategory(Category.DAILY_HOBBY);
        request.setTitle(title);
        request.setContent("테스트 내용");
        request.setEndTime(LocalDateTime.now().plusDays(7));
        request.setAllowRevote(false);
        request.setAllowMultipleChoice(false);

        List<VoteOptionRequest> voteOptions = new ArrayList<>();
        VoteOptionRequest option1 = new VoteOptionRequest();
        option1.setContent("옵션 A");
        VoteOptionRequest option2 = new VoteOptionRequest();
        option2.setContent("옵션 B");
        voteOptions.add(option1);
        voteOptions.add(option2);
        request.setVoteOptions(voteOptions);

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        MvcResult result = mockMvc.perform(multipart("/community/votesboard")
                        .file(data)
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("votesboardId").asLong();
    }

    private Long createComment(Long votePostId, CustomUserDetails userDetails, String content) throws Exception {
        VoteboardCommentCreateRequest request = VoteboardCommentCreateRequest.builder()
                .content(content)
                .build();

        MvcResult result = mockMvc.perform(post("/community/votesboard/" + votePostId + "/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("commentId").asLong();
    }
}
