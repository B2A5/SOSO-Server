package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.util.TestUserHelper;
import com.example.soso.community.freeboard.util.TestUserHelper.TestUser;
import com.example.soso.community.common.comment.domain.dto.*;
import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.config.TestS3Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Import(TestS3Config.class)
@DisplayName("⚡ 성능 및 커서 페이징 통합 테스트")
class PerformancePaginationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestUserHelper testUserHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("🏃‍♂️ 대용량 데이터 커서 페이징 성능 테스트: 20개 게시글로 페이징 시나리오")
    void largeCursorPaginationPerformanceTest() throws Exception {
        System.out.println("\n🎬 === 대용량 데이터 커서 페이징 성능 테스트 ===");
        System.out.println("시나리오: 20개의 게시글을 생성하고 커서 기반 페이징으로 순회");

        // ==================== STEP 1: 테스트 데이터 대량 생성 ====================
        System.out.println("\n[STEP 1] 20개의 다양한 게시글 생성...");

        TestUser[] authors = new TestUser[5];
        for (int i = 0; i < 5; i++) {
            authors[i] = (i % 2 == 0) ? testUserHelper.createFounderUser() : testUserHelper.createInhabitantUser();
            System.out.println("작성자 " + (i+1) + ": " + authors[i].getNickname() + " (" + authors[i].getUserType() + ")");
        }

        List<Long> postIds = new ArrayList<>();
        String[] categories = {"RESTAURANT", "DAILY_HOBBY", "LIVING_CONVENIENCE", "STARTUP", "OTHERS"};

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            TestUser author = authors[i % 5]; // 순환하며 작성자 배정
            String category = categories[i % 5]; // 순환하며 카테고리 배정

            MockMultipartFile imageFile = new MockMultipartFile(
                    "images", "post" + i + ".jpg", "image/jpeg",
                    ("게시글 " + i + " 이미지").getBytes()
            );

            String title = String.format("🎯 [%s] 게시글 %d번 - %s의 실전 경험담",
                    category, i, author.getNickname());
            String content = String.format(
                    "안녕하세요! %s입니다.\n\n" +
                    "게시글 %d번째로, %s 카테고리에 대한 경험을 공유합니다.\n\n" +
                    "✨ 주요 내용:\n" +
                    "1️⃣ 실전에서 겪은 경험\n" +
                    "2️⃣ 성공과 실패 사례\n" +
                    "3️⃣ 앞으로의 계획\n\n" +
                    "많은 관심과 댓글 부탁드려요! 🚀\n" +
                    "(생성 시간: %d번째)",
                    author.getNickname(), i, category, i
            );

            MvcResult result = mockMvc.perform(multipart("/community/freeboard")
                            .file(imageFile)
                            .param("title", title)
                            .param("content", content)
                            .param("category", category)
                            .header("Authorization", author.getAuthHeader())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andReturn();

            FreeboardCreateResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), FreeboardCreateResponse.class);
            postIds.add(response.getPostId());

            if (i % 5 == 0) {
                System.out.println("✅ " + i + "개 게시글 생성 완료...");
            }
        }

        long creationTime = System.currentTimeMillis() - startTime;
        System.out.println("✅ 총 20개 게시글 생성 완료! (소요시간: " + creationTime + "ms)");

        // ==================== STEP 2: 첫 페이지 조회 (최신순) ====================
        System.out.println("\n[STEP 2] 첫 페이지 조회 (최신순, 5개씩)...");

        startTime = System.currentTimeMillis();

        MvcResult firstPageResult = mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "5")
                        .header("Authorization", authors[0].getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(20))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn();

        long firstPageTime = System.currentTimeMillis() - startTime;

        FreeboardCursorResponse firstPage = objectMapper.readValue(
                firstPageResult.getResponse().getContentAsString(), FreeboardCursorResponse.class);

        System.out.println("✅ 첫 페이지 조회 성공! (소요시간: " + firstPageTime + "ms)");
        System.out.println("  - 게시글 수: " + firstPage.getSize());
        System.out.println("  - 다음 페이지 존재: " + firstPage.isHasNext());
        System.out.println("  - 총 게시글 수: " + firstPage.getTotalCount());
        System.out.println("  - 다음 커서: " + firstPage.getNextCursor());

        // ==================== STEP 3: 커서 기반 다음 페이지 조회 ====================
        System.out.println("\n[STEP 3] 커서 기반 다음 페이지들 순회...");

        String currentCursor = firstPage.getNextCursor();
        int pageNumber = 2;
        int totalRetrieved = 5;

        while (currentCursor != null && pageNumber <= 5) { // 최대 5페이지까지 테스트
            System.out.println("\n  [페이지 " + pageNumber + "] 커서로 조회: " + currentCursor);

            startTime = System.currentTimeMillis();

            MvcResult pageResult = mockMvc.perform(get("/community/freeboard")
                            .param("sort", "LATEST")
                            .param("size", "5")
                            .param("cursor", currentCursor)
                            .header("Authorization", authors[0].getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.totalCount").value(20))
                    .andReturn();

            long pageTime = System.currentTimeMillis() - startTime;

            FreeboardCursorResponse pageResponse = objectMapper.readValue(
                    pageResult.getResponse().getContentAsString(), FreeboardCursorResponse.class);

            totalRetrieved += pageResponse.getSize();
            currentCursor = pageResponse.getNextCursor();

            System.out.println("    ✅ 페이지 " + pageNumber + " 조회 완료! (소요시간: " + pageTime + "ms)");
            System.out.println("      - 이번 페이지 게시글 수: " + pageResponse.getSize());
            System.out.println("      - 누적 조회 게시글 수: " + totalRetrieved);
            System.out.println("      - 다음 페이지 존재: " + pageResponse.isHasNext());

            pageNumber++;
        }

        System.out.println("\n✅ 커서 페이징 완료! 총 " + totalRetrieved + "개 게시글 조회");

        // ==================== STEP 4: 다양한 정렬 옵션 성능 테스트 ====================
        System.out.println("\n[STEP 4] 다양한 정렬 옵션 성능 비교...");

        String[] sortOptions = {"LATEST", "LIKE", "COMMENT", "VIEW"};

        for (String sort : sortOptions) {
            System.out.println("\n  [" + sort + " 정렬] 성능 측정...");

            startTime = System.currentTimeMillis();

            mockMvc.perform(get("/community/freeboard")
                            .param("sort", sort)
                            .param("size", "10")
                            .header("Authorization", authors[0].getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.totalCount").value(20));

            long sortTime = System.currentTimeMillis() - startTime;
            System.out.println("    ✅ " + sort + " 정렬 완료! (소요시간: " + sortTime + "ms)");
        }

        System.out.println("\n🎉 === 대용량 데이터 커서 페이징 성능 테스트 완료 ===");
    }

    @Test
    @DisplayName("📊 카테고리별 필터링 성능 테스트: 각 카테고리별 게시글 조회")
    void categoryFilteringPerformanceTest() throws Exception {
        System.out.println("\n🎬 === 카테고리별 필터링 성능 테스트 ===");

        // ==================== STEP 1: 카테고리별 테스트 데이터 생성 ====================
        TestUser author = testUserHelper.createFounderUser();
        String[] categories = {"RESTAURANT", "DAILY_HOBBY", "LIVING_CONVENIENCE", "STARTUP", "OTHERS"};

        System.out.println("\n[STEP 1] 각 카테고리별로 3개씩 게시글 생성...");

        for (String category : categories) {
            for (int i = 1; i <= 3; i++) {
                MockMultipartFile imageFile = new MockMultipartFile(
                        "images", category + i + ".jpg", "image/jpeg",
                        (category + " " + i + " 이미지").getBytes()
                );

                String title = String.format("[%s] 카테고리 테스트 게시글 %d", category, i);
                String content = String.format("%s 카테고리의 %d번째 테스트 게시글입니다.", category, i);

                mockMvc.perform(multipart("/community/freeboard")
                                .file(imageFile)
                                .param("title", title)
                                .param("content", content)
                                .param("category", category)
                                .header("Authorization", author.getAuthHeader())
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                        .andExpect(status().isOk());
            }
            System.out.println("✅ " + category + " 카테고리 3개 게시글 생성 완료");
        }

        System.out.println("✅ 총 15개 게시글 생성 완료 (카테고리별 3개씩)");

        // ==================== STEP 2: 각 카테고리별 필터링 성능 측정 ====================
        System.out.println("\n[STEP 2] 각 카테고리별 필터링 성능 측정...");

        for (String category : categories) {
            System.out.println("\n  [" + category + "] 카테고리 필터링...");

            long startTime = System.currentTimeMillis();

            MvcResult result = mockMvc.perform(get("/community/freeboard")
                            .param("category", category)
                            .param("sort", "LATEST")
                            .param("size", "10")
                            .header("Authorization", author.getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.posts.length()").value(3)) // 각 카테고리당 3개
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andReturn();

            long filterTime = System.currentTimeMillis() - startTime;

            FreeboardCursorResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), FreeboardCursorResponse.class);

            System.out.println("    ✅ " + category + " 필터링 완료! (소요시간: " + filterTime + "ms)");
            System.out.println("      - 조회된 게시글 수: " + response.getSize());
            System.out.println("      - 모든 게시글이 " + category + " 카테고리인지 확인");

            // 모든 게시글이 해당 카테고리인지 검증
            response.getPosts().forEach(post -> {
                if (!post.getCategory().name().equals(category)) {
                    throw new AssertionError("카테고리 필터링 실패: 예상=" + category + ", 실제=" + post.getCategory());
                }
            });
        }

        // ==================== STEP 3: 전체 카테고리 조회 (category 파라미터 없음) ====================
        System.out.println("\n[STEP 3] 전체 카테고리 조회 성능 측정...");

        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "20")
                        .header("Authorization", author.getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts.length()").value(15)) // 모든 게시글
                .andExpect(jsonPath("$.totalCount").value(15));

        long allCategoryTime = System.currentTimeMillis() - startTime;
        System.out.println("✅ 전체 카테고리 조회 완료! (소요시간: " + allCategoryTime + "ms)");

        System.out.println("\n🎉 === 카테고리별 필터링 성능 테스트 완료 ===");
    }

    @Test
    @DisplayName("💬 댓글 페이징 성능 테스트: 많은 댓글이 있는 게시글의 댓글 페이징")
    void commentPaginationPerformanceTest() throws Exception {
        System.out.println("\n🎬 === 댓글 페이징 성능 테스트 ===");

        // ==================== STEP 1: 게시글 생성 ====================
        TestUser postAuthor = testUserHelper.createFounderUser();

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "popular-post.jpg", "image/jpeg", "인기 게시글 이미지".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "🔥 댓글 대폭발! 인기 게시글 - 모두의 관심사")
                        .param("content", "이 게시글은 많은 댓글을 받을 예정인 인기 게시글입니다!\n" +
                                "댓글 페이징 테스트를 위해 생성되었습니다.\n" +
                                "많은 댓글 부탁드려요! 💬")
                        .param("category", "OTHERS")
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long postId = postResponse.getPostId();

        System.out.println("✅ 인기 게시글 생성 완료: ID=" + postId);

        // ==================== STEP 2: 15개의 댓글 생성 ====================
        System.out.println("\n[STEP 2] 15개의 다양한 댓글 생성...");

        TestUser[] commenters = new TestUser[5];
        for (int i = 0; i < 5; i++) {
            commenters[i] = (i % 2 == 0) ? testUserHelper.createFounderUser() : testUserHelper.createInhabitantUser();
        }

        List<Long> commentIds = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= 15; i++) {
            TestUser commenter = commenters[i % 5];

            String commentContent = String.format(
                    "댓글 %d번입니다! 🎉\n" +
                    "%s가 작성했습니다.\n" +
                    "정말 유익한 게시글이네요! 👍\n" +
                    "더 많은 정보 공유 부탁드려요~",
                    i, commenter.getNickname()
            );

            MvcResult commentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                            .header("Authorization", commenter.getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"" + commentContent.replace("\n", "\\n") + "\"}"))
                    .andExpect(status().isCreated())
                    .andReturn();

            FreeboardCommentCreateResponse response = objectMapper.readValue(
                    commentResult.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
            commentIds.add(response.getCommentId());

            if (i % 5 == 0) {
                System.out.println("  ✅ " + i + "개 댓글 생성 완료...");
            }
        }

        long commentCreationTime = System.currentTimeMillis() - startTime;
        System.out.println("✅ 총 15개 댓글 생성 완료! (소요시간: " + commentCreationTime + "ms)");

        // ==================== STEP 3: 댓글 첫 페이지 조회 ====================
        System.out.println("\n[STEP 3] 댓글 첫 페이지 조회 (5개씩)...");

        startTime = System.currentTimeMillis();

        MvcResult firstCommentPageResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST")
                        .param("size", "5")
                        .header("Authorization", commenters[0].getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn();

        long firstCommentPageTime = System.currentTimeMillis() - startTime;

        FreeboardCommentCursorResponse firstCommentPage = objectMapper.readValue(
                firstCommentPageResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("✅ 댓글 첫 페이지 조회 성공! (소요시간: " + firstCommentPageTime + "ms)");
        System.out.println("  - 댓글 수: " + firstCommentPage.getSize());
        System.out.println("  - 다음 페이지 존재: " + firstCommentPage.isHasNext());

        // ==================== STEP 4: 댓글 커서 페이징 순회 ====================
        System.out.println("\n[STEP 4] 댓글 커서 페이징 순회...");

        String commentCursor = firstCommentPage.getNextCursor();
        int commentPageNumber = 2;
        int totalCommentsRetrieved = 5;

        while (commentCursor != null && commentPageNumber <= 4) { // 최대 4페이지
            System.out.println("\n  [댓글 페이지 " + commentPageNumber + "] 커서로 조회...");

            startTime = System.currentTimeMillis();

            MvcResult commentPageResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                            .param("sort", "LATEST")
                            .param("size", "5")
                            .param("cursor", commentCursor)
                            .header("Authorization", commenters[0].getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray())
                    .andReturn();

            long commentPageTime = System.currentTimeMillis() - startTime;

            FreeboardCommentCursorResponse commentPageResponse = objectMapper.readValue(
                    commentPageResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

            totalCommentsRetrieved += commentPageResponse.getSize();
            commentCursor = commentPageResponse.getNextCursor();

            System.out.println("    ✅ 댓글 페이지 " + commentPageNumber + " 조회 완료! (소요시간: " + commentPageTime + "ms)");
            System.out.println("      - 이번 페이지 댓글 수: " + commentPageResponse.getSize());
            System.out.println("      - 누적 조회 댓글 수: " + totalCommentsRetrieved);

            commentPageNumber++;
        }

        System.out.println("\n✅ 댓글 페이징 완료! 총 " + totalCommentsRetrieved + "개 댓글 조회");

        System.out.println("\n🎉 === 댓글 페이징 성능 테스트 완료 ===");
    }

    @Test
    @DisplayName("🎯 커서 정확성 및 중복 방지 테스트: 페이징 중 데이터 변경 시나리오")
    void cursorAccuracyAndDuplicationPreventionTest() throws Exception {
        System.out.println("\n🎬 === 커서 정확성 및 중복 방지 테스트 ===");
        System.out.println("시나리오: 페이징 조회 중간에 새 게시글이 추가되어도 중복 조회되지 않는지 확인");

        // ==================== STEP 1: 초기 게시글 10개 생성 ====================
        TestUser author1 = testUserHelper.createFounderUser();
        TestUser author2 = testUserHelper.createInhabitantUser();

        System.out.println("\n[STEP 1] 초기 게시글 10개 생성...");

        for (int i = 1; i <= 10; i++) {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "images", "initial" + i + ".jpg", "image/jpeg", ("초기 게시글 " + i).getBytes()
            );

            mockMvc.perform(multipart("/community/freeboard")
                            .file(imageFile)
                            .param("title", "초기 게시글 " + i + "번")
                            .param("content", "커서 테스트를 위한 초기 게시글 " + i + "번입니다.")
                            .param("category", "OTHERS")
                            .header("Authorization", author1.getAuthHeader())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());

            // 시간 차이를 두어 명확한 정렬 순서 보장
            Thread.sleep(10);
        }

        System.out.println("✅ 초기 10개 게시글 생성 완료");

        // ==================== STEP 2: 첫 페이지 조회 (5개) ====================
        System.out.println("\n[STEP 2] 첫 페이지 조회 (5개)...");

        MvcResult firstPageResult = mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "5")
                        .header("Authorization", author1.getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalCount").value(10))
                .andReturn();

        FreeboardCursorResponse firstPage = objectMapper.readValue(
                firstPageResult.getResponse().getContentAsString(), FreeboardCursorResponse.class);

        List<Long> firstPageIds = firstPage.getPosts().stream()
                .map(FreeboardCursorResponse.FreeboardSummary::getPostId)
                .toList();

        System.out.println("✅ 첫 페이지 조회 완료");
        System.out.println("  - 첫 페이지 게시글 ID들: " + firstPageIds);
        System.out.println("  - 다음 커서: " + firstPage.getNextCursor());

        // ==================== STEP 3: 페이징 중간에 새 게시글 3개 추가 ====================
        System.out.println("\n[STEP 3] 페이징 조회 중간에 새 게시글 3개 추가...");

        for (int i = 11; i <= 13; i++) {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "images", "new" + i + ".jpg", "image/jpeg", ("새 게시글 " + i).getBytes()
            );

            mockMvc.perform(multipart("/community/freeboard")
                            .file(imageFile)
                            .param("title", "🆕 새 게시글 " + i + "번 (페이징 중 추가)")
                            .param("content", "페이징 테스트 중간에 추가된 게시글 " + i + "번입니다.")
                            .param("category", "OTHERS")
                            .header("Authorization", author2.getAuthHeader())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());

            Thread.sleep(10);
        }

        System.out.println("✅ 새 게시글 3개 추가 완료 (총 13개 게시글)");

        // ==================== STEP 4: 기존 커서로 두 번째 페이지 조회 ====================
        System.out.println("\n[STEP 4] 기존 커서로 두 번째 페이지 조회...");

        MvcResult secondPageResult = mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "5")
                        .param("cursor", firstPage.getNextCursor()) // 기존 커서 사용
                        .header("Authorization", author1.getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.totalCount").value(13)) // 총 개수는 업데이트됨
                .andReturn();

        FreeboardCursorResponse secondPage = objectMapper.readValue(
                secondPageResult.getResponse().getContentAsString(), FreeboardCursorResponse.class);

        List<Long> secondPageIds = secondPage.getPosts().stream()
                .map(FreeboardCursorResponse.FreeboardSummary::getPostId)
                .toList();

        System.out.println("✅ 두 번째 페이지 조회 완료");
        System.out.println("  - 두 번째 페이지 게시글 ID들: " + secondPageIds);
        System.out.println("  - 조회된 게시글 수: " + secondPage.getSize());

        // ==================== STEP 5: 중복 조회 검증 ====================
        System.out.println("\n[STEP 5] 중복 조회 방지 검증...");

        boolean hasDuplication = firstPageIds.stream()
                .anyMatch(secondPageIds::contains);

        if (hasDuplication) {
            throw new AssertionError("❌ 중복 조회 발생! 첫 페이지와 두 번째 페이지에 동일한 게시글이 있습니다.");
        }

        System.out.println("✅ 중복 조회 방지 성공!");
        System.out.println("  - 첫 페이지와 두 번째 페이지에 중복되는 게시글 없음");
        System.out.println("  - 커서 기반 페이징이 정확히 작동");

        // ==================== STEP 6: 전체 데이터 일관성 확인 ====================
        System.out.println("\n[STEP 6] 전체 데이터 일관성 확인...");

        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "20") // 모든 게시글 조회
                        .header("Authorization", author1.getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts.length()").value(13))
                .andExpect(jsonPath("$.totalCount").value(13))
                .andExpect(jsonPath("$.hasNext").value(false));

        System.out.println("✅ 전체 데이터 일관성 확인 완료!");

        System.out.println("\n🎉 === 커서 정확성 및 중복 방지 테스트 완료 ===");
        System.out.println("📊 테스트 결과:");
        System.out.println("  - 페이징 중 데이터 추가되어도 중복 조회 없음");
        System.out.println("  - 커서 기반 페이징 정확성 보장");
        System.out.println("  - 총 게시글 수 실시간 반영");
    }
}