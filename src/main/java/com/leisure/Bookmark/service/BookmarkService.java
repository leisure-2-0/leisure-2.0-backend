package com.leisure.Bookmark.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import com.leisure.Bookmark.repository.BookmarkRepository;
import com.leisure.Bookmark.domain.PostBookmark;
import com.leisure.Bookmark.dto.response.BookmarkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final MemberReader reader;

    private final PostRepository postRepository;

    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public BookmarkResponse bookmark(String publicId, Long postId) {

        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();
        Post post = getPublishedPost(postId);

        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndPostId(memberId, post.getPostId());

        if (isBookmarked) {
            throw new BusinessException(ErrorCode.POST_BOOKMARK_ALREADY_BOOKMARKED);
        }

        PostBookmark bookmark = PostBookmark.of(memberId, post.getPostId());

        try {
            bookmarkRepository.save(bookmark);
            bookmarkRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.POST_BOOKMARK_ALREADY_BOOKMARKED);
        }

        postRepository.increaseBookmarkCount(post.getPostId());

        int bookmarkCount = postRepository.findBookmarkCountByPostId(post.getPostId());

        return new BookmarkResponse(memberId, post.getPostId(), bookmarkCount, true);
    }

    @Transactional
    public BookmarkResponse unbookmark(String publicId, Long postId) {

        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();
        Post post = getPublishedPost(postId);

        int deleted = bookmarkRepository.deleteByMemberIdAndPostId(memberId, post.getPostId());

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.POST_BOOKMARK_NOT_BOOKMARKED_YET);
        }

        postRepository.decreaseBookmarkCount(post.getPostId());

        int bookmarkCount = postRepository.findBookmarkCountByPostId(post.getPostId());

        return new BookmarkResponse(memberId, post.getPostId(), bookmarkCount, false);
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
