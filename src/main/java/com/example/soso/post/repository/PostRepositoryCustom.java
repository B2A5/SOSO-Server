package com.example.soso.post.repository;


import com.example.soso.post.domain.dto.PostSortType;
import com.example.soso.post.domain.entity.Category;
import com.example.soso.post.domain.entity.Post;

import java.util.List;

public interface PostRepositoryCustom {

    /**
     * 카테고리와 정렬 기준에 따라 커서 기반 게시글 목록 조회
     *
     * @param category 카테고리 (null이면 전체)
     * @param sort     정렬 기준 (LATEST, LIKE, COMMENT)
     * @param size     요청 개수
     * @param cursor   정렬 기준 커서 값
     * @param idAfter  보조 정렬용 ID (동일 정렬값일 경우)
     * @return 게시글 목록 (size + 1개 조회)
     */
    List<Post> findAllByCursorPaging(Category category, PostSortType sort, int size, String cursor, Long idAfter);
}
