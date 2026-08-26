package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostLocation;
import com.leisure.post.dto.request.LocationRequest;
import com.leisure.post.dto.response.PostDeleteResponse;
import com.leisure.post.dto.request.PostEditRequest;
import com.leisure.post.dto.request.PostPublishRequest;
import com.leisure.post.dto.request.PostSaveRequest;
import com.leisure.post.dto.response.PostEditResponse;
import com.leisure.post.dto.response.PostPublishResponse;
import com.leisure.post.dto.response.PostSaveResponse;
import com.leisure.post.dto.response.PostStartResponse;
import com.leisure.post.repository.PostRepository;

import com.leisure.tag.domain.PostTag;
import com.leisure.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostService {

    private final MemberReader reader;

    private final PostRepository repository;

    private final TagRepository tagRepository;


    @Transactional
    public PostStartResponse startPosting(String publicId) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        Post post = Post.startWriting(memberId);

        repository.save(post);

        return new PostStartResponse(post.getPostId(), post.getStatus());
    }

    @Transactional
    public PostSaveResponse saveDraft(String publicId, Long postId, PostSaveRequest request) {
        Post post = getOwnedPost(publicId, postId);

        post.applyContent(request.title(), request.content(), request.category(), toLocation(request.location()));

        if (request.tags() != null) {
            replaceTags(post.getPostId(), request.tags());
        }

        post.markAsDraft();

        return new PostSaveResponse(post.getPostId(), post.getStatus());
    }

    @Transactional
    public PostPublishResponse publish(String publicId, Long postId, PostPublishRequest request) {

        Post post = getOwnedPost(publicId, postId);

        post.applyContent(request.title(), request.content(), request.category(), toLocation(request.location()));

        if (request.tags() != null) {
            replaceTags(post.getPostId(), request.tags());
        }

        post.publish();

        return new PostPublishResponse(post.getPostId(), post.getStatus(), post.getPublishedAt());
    }


    @Transactional
    public PostEditResponse editPost(String publicId, Long postId, PostEditRequest request) {

        Post post = getOwnedPost(publicId, postId);

        post.editPublished(request.title(), request.content(), request.category(), toLocation(request.location()));

        if (request.tags() != null) {
            replaceTags(post.getPostId(), request.tags());
        }

        return new PostEditResponse(post.getPostId());
    }

    @Transactional
    public PostDeleteResponse deletePost(String publicId, Long postId) {

        Post post = getOwnedPost(publicId, postId);

        post.delete();

        return new PostDeleteResponse(post.getPostId());
    }


    private Post getOwnedPost(String publicId, Long postId) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        Post post = repository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }

        return post;
    }

    private PostLocation toLocation(LocationRequest request) {
        return request == null ? null : request.toPostLocation();
    }

    private void replaceTags(Long postId, Set<String> tagNames) {
        tagRepository.deleteByPostId(postId);

        if (tagNames.isEmpty()) {
            return;
        }

        tagRepository.saveAll(PostTag.createAll(postId, tagNames));
    }
}
