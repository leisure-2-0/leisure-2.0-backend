package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.assembler.PostResponseAssembler;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostCursor;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.*;
import com.leisure.post.dto.result.MainFeedPostResult;
import com.leisure.post.dto.result.MyPostResult;
import com.leisure.post.dto.result.PostResult;
import com.leisure.post.dto.result.PostDetailResult;
import com.leisure.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final MemberReader reader;

    private final PostRepository repository;

    private final ObjectMapper objectMapper;

    private final PostResponseAssembler assembler;

    @Transactional(readOnly = true)
    public MyPostListResponse getMyPosts(String publicId, MyPostSort sort, Integer page, Integer size) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        int pageNumber = validatePage(page);
        int pageSize = validateSize(size);

        long offset = (long) pageNumber * pageSize;

        List<MyPostResult> results = repository.findMyPosts(memberId, sort, offset, pageSize);

        List<MyPostResponse> myPosts = assembler.assembleMyPosts(results);

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
            return 15;
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

    @Transactional(readOnly = true)
    public PostListResponse getPosts(String publicId, PostCategory category, PostSort sort, String cursor, Integer limit) {

        Long memberId = null;
        if (publicId != null) {
            memberId = reader.getMemberByPublicId(publicId).getMemberId();
        }

        int validLimit = validateLimit(limit);

        PostCursor postCursor = decodeCursor(sort, cursor);

        List<PostResult> results = repository.findPosts(memberId, category, sort, postCursor, validLimit + 1);

        boolean hasNext = results.size() > validLimit;

        if (hasNext) {
            results = new ArrayList<>(results.subList(0, validLimit));
        }

        String nextCursor = createNextCursor(results, hasNext, sort);

        List<PostResponse> posts = assembler.assemblePosts(results);

        return new PostListResponse(posts, nextCursor, hasNext);
    }

    private int validateLimit(Integer limit) {

        if (limit == null) {
            limit = 15;
        }

        if (limit < 1 || limit > 30) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_INVALID);
        }

        return limit;
    }

    private PostCursor decodeCursor(PostSort sort, String cursor) {

        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            String json = new String(bytes, StandardCharsets.UTF_8);

            PostCursor postCursor = objectMapper.readValue(json, PostCursor.class);

            if (postCursor.postId() == null) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR);
            }

            if (sort == PostSort.POPULAR && postCursor.likeCount() == null) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR);
            }

            if (sort != PostSort.POPULAR && postCursor.publishedAt() == null) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR);
            }

            return postCursor;
        } catch (IllegalArgumentException | JacksonException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private String createNextCursor(List<PostResult> posts, boolean hasNext, PostSort sort) {

        if (!hasNext || posts.isEmpty()) {
            return null;
        }

        try {
            PostResult lastPost = posts.get(posts.size() - 1);

            PostCursor nextCursor = createPostCursor(lastPost, sort);

            String nextJson = objectMapper.writeValueAsString(nextCursor);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(nextJson.getBytes(StandardCharsets.UTF_8));

        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private PostCursor createPostCursor(PostResult post, PostSort sort) {
        if (sort == PostSort.POPULAR) {
            return new PostCursor(post.postId(), null, post.likeCount());
        }

        return new PostCursor(post.postId(), post.publishedAt(), null);
    }

    @Transactional(readOnly = true)
    public List<MainFeedPostResponse> getMainFeedPosts(String publicId, PostCategory category, PostSort sort) {

        Long memberId = null;

        if (publicId != null) {
            memberId = reader.getMemberByPublicId(publicId).getMemberId();
        }

        List<MainFeedPostResult> results = repository.findMainFeedPosts(memberId, category, sort, 18);

        return assembler.assembleMainFeed(results);
    }


    @Transactional
    public PostDetailResponse getPostDetail(String publicId, Long postId) {

        Long memberId = null;

        if (publicId != null) {
            memberId = reader.getMemberByPublicId(publicId).getMemberId();
        }

        PostDetailResult result = repository.findPostDetail(memberId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // TODO: 부하 테스트 후 Redis 조회수 INCR
        repository.increaseViewCount(result.postId());

        return assembler.assembleDetail(result);
    }
}