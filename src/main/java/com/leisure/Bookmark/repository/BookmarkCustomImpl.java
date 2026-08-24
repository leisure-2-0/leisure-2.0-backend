package com.leisure.Bookmark.repository;

import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.response.BookmarkedPostResponse;
import com.leisure.post.domain.PostStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.leisure.Bookmark.domain.QPostBookmark.postBookmark;
import static com.leisure.member.domain.QMember.member;
import static com.leisure.post.domain.QPost.post;
import static com.leisure.postLike.domain.QPostLike.postLike;

@Repository
@RequiredArgsConstructor
public class BookmarkCustomImpl implements BookmarkCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<BookmarkedPostResponse> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size) {
        return queryFactory
                .select(
                        Projections.constructor(
                                BookmarkedPostResponse.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                post.memberId.eq(memberId),
                                postLike.postLikeId.isNotNull(),
                                postBookmark.memberId.eq(memberId),
                                post.location.region,
                                post.publishedAt,
                                postBookmark.createdAt,
                                Projections.constructor(
                                        BookmarkedPostResponse.AuthorResponse.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                )
                        )
                )
                .from(postBookmark)
                .join(post)
                .on(postBookmark.postId.eq(post.postId))
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postLike)
                .on(
                        postLike.postId.eq(post.postId),
                        postLike.memberId.eq(memberId)
                )
                .where(
                        postBookmark.memberId.eq(memberId),
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED)
                )
                .orderBy(orderBy(sort))
                .offset(offset)
                .limit(size)
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(BookmarkedPostSort sort) {
        if (sort == BookmarkedPostSort.POPULAR) {
            return new OrderSpecifier[]{
                    post.bookmarkCount.desc(),
                    post.postId.desc()
            };
        }

        return new OrderSpecifier[]{
                postBookmark.createdAt.desc(),
                postBookmark.postBookmarkId.desc()
        };
    }
}
