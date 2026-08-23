package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.Post;
import com.leisure.post.dto.response.MyPostListResponse;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostDetailResponse;
import com.leisure.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final MemberReader reader;

    private final PostRepository repository;

    @Transactional(readOnly = true)
    public MyPostListResponse getMyPosts(String publicId, MyPostSort sort, Integer page, Integer size) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        int pageNumber = validatePage(page);
        int pageSize = validateSize(size);

        long offset = (long) pageNumber * pageSize;

        List<MyPostResponse> myPosts = repository.findMyPosts(memberId, sort, offset, pageSize);

        long totalElements = repository.countMyPosts(memberId);

        int totalPages = calculateTotalPages(totalElements, pageSize);

        boolean hasNext = pageNumber + 1 < totalPages;

        return new MyPostListResponse(myPosts, pageNumber, pageSize, totalElements, totalPages, hasNext);
    }

    private int validatePage(Integer page) {
        if (page == null) {
            return 0;
        }

        if (page < 0) {
            throw new BusinessException(ErrorCode.PAGE_INVALID);
        }

        return page;
    }

    private int validateSize(Integer size) {
        if (size == null) {
            return 10;
        }

        if (size < 1 || size > 30) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_INVALID);
        }

        return size;
    }

    private int calculateTotalPages(long totalElements, int size) {
        double result = (double) totalElements / size;
        return (int) Math.ceil(result);
    }

    // TODO: 게시글 목록/피드 조회




    // TODO: 특정 게시글 조회
//    @Transactional(readOnly = true)
//    public PostDetailResponse getPostDetail(String publicId, Long postId) {
//
//        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();
//
//        PostDetailResponse postDetailResponse = repository.findPostDetail(memberId, postId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
//
//
//        // TODO: Redis INCR
//        return postDetailResponse;
//    }
}
