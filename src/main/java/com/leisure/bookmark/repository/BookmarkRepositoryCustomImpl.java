package com.leisure.bookmark.repository;

import com.leisure.bookmark.domain.BookmarkedPostSort;
import com.leisure.bookmark.dto.result.BookmarkedPostResult;
import com.leisure.post.domain.PostStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.leisure.bookmark.domain.QPostBookmark.postBookmark;
import static com.leisure.member.domain.QMember.member;
import static com.leisure.post.domain.QPost.post;
import static com.leisure.postlike.domain.QPostLike.postLike;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryCustomImpl implements BookmarkRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<BookmarkedPostResult> findBookmarkedPosts(Long memberId, BookmarkedPostSort sort, long offset, int size) {
        return queryFactory
                .select(
                        Projections.constructor(
                                BookmarkedPostResult.class,
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
                                        BookmarkedPostResult.AuthorResult.class,
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
