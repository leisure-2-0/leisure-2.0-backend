package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.event.PostPublishedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostService {

    private final MemberReader reader;

    private final PostRepository repository;

    private final TagRepository tagRepository;

    private final ApplicationEventPublisher eventPublisher;


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

        eventPublisher.publishEvent(new PostPublishedEvent(post.getMemberId(), post.getPostId()));

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

        // 소유권 확인 (없으면 POST_NOT_FOUND, 남의 글이면 POST_FORBIDDEN)
        Post post = getOwnedPost(publicId, postId);

        if (post.isDraft()) {
            // 초안(WRITING/DRAFT): 게시된 적 없어 좋아요/북마크 참조가 없으므로 즉시 하드 삭제한다.
            // 자식인 태그만 정리한 뒤 글을 물리 삭제 (같은 트랜잭션이라 원자적)
            tagRepository.deleteByPostId(postId);
            repository.delete(post);
        } else {
            // 게시글(PUBLISHED): 소프트 삭제로 즉시 숨긴다 (deleted_at 기록, 각 조회 쿼리의 명시 필터로 제외)
            // TODO: 소프트 삭제된 게시글은 배치로 일괄 하드 삭제하고,
            //       태그, 좋아요, 북마크도 같은 생명주기로 함께 배치 삭제한다.
            post.delete();
        }

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
