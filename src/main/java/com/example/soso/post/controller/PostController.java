package com.example.soso.post.controller;

import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.service.PostService;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;


    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createPost(@ModelAttribute PostCreateRequest request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Users user = userDetails.getUser();
        Long postId = postService.createPost(request, user);
        return ResponseEntity.ok(postId);
    }


}
