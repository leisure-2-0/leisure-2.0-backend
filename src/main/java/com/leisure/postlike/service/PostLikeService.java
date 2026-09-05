package com.leisure.postlike.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import com.leisure.postlike.repository.PostLikeRepository;
import com.leisure.postlike.domain.PostLike;
import com.leisure.postlike.dto.response.PostLikeResponse;
import com.leisure.postlike.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final MemberReader reader;

    private final PostRepository postRepository;

    private final PostLikeRepository likeRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostLikeResponse like(String publicId, Long postId) {

        Member member = reader.getMemberByPublicId(publicId);
        Post post = getPublishedPost(postId);

        boolean isLiked = likeRepository.existsByMemberIdAndPostId(member.getMemberId(), post.getPostId());

        if (isLiked) {
            throw new BusinessException(ErrorCode.POST_LIKE_ALREADY_LIKED);
        }

        PostLike postLike = PostLike.of(member.getMemberId(), post.getPostId());

        try {
            likeRepository.save(postLike);
            likeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.POST_LIKE_ALREADY_LIKED);
        }

        postRepository.increaseLikeCount(post.getPostId());

        int likeCount = postRepository.findLikeCountByPostId(post.getPostId());

        eventPublisher.publishEvent(new PostLikedEvent(post.getMemberId(), member.getMemberId(), post.getPostId()));

        return new PostLikeResponse(member.getMemberId(), post.getPostId(), likeCount, true);
    }

    @Transactional
    public PostLikeResponse unlike(String publicId, Long postId) {

        Member member = reader.getMemberByPublicId(publicId);
        Post post = getPublishedPost(postId);

//        PostLike postLike = likeRepository.findByMemberIdAndPostId(member.getMemberId(), post.getPostId())
//                .orElseThrow(() -> new BusinessException(ErrorCode.POST_LIKE_NOT_LIKED_YET));
//
//        likeRepository.delete(postLike);

        int deleted = likeRepository.deleteByMemberIdAndPostId(member.getMemberId(), post.getPostId());

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.POST_LIKE_NOT_LIKED_YET);
        }

        postRepository.decreaseLikeCount(post.getPostId());

        int likeCount = postRepository.findLikeCountByPostId(post.getPostId());

        return new PostLikeResponse(member.getMemberId(), post.getPostId(), likeCount, false);
    }

    private Post getPublishedPost(Long postId) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);

        }

        return post;
    }
}
