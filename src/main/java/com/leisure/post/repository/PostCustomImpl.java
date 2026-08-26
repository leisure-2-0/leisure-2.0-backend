package com.leisure.post.repository;

import com.leisure.post.domain.*;
import com.leisure.post.dto.response.DraftListResponse;
import com.leisure.post.dto.result.*;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.leisure.Bookmark.domain.QPostBookmark.postBookmark;
import static com.leisure.member.domain.QMember.member;
import static com.leisure.post.domain.QPost.post;
import static com.leisure.postLike.domain.QPostLike.postLike;

@Repository
@RequiredArgsConstructor
public class PostCustomImpl implements PostCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyPostResult> findMyPosts(Long memberId, MyPostSort sort, long offset, int size) {
        return queryFactory.select(
                        Projections.constructor(
                                MyPostResult.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                post.memberId.eq(memberId),
                                postLike.postLikeId.isNotNull(),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.location.region,
                                post.publishedAt,
                                post.createdAt,
                                post.updatedAt,
                                Projections.constructor(
                                        MyPostResult.AuthorResult.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                )
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postLike)
                .on(
                        post.postId.eq(postLike.postId),
                        postLike.memberId.eq(memberId)
                )
                .leftJoin(postBookmark)
                .on(
                        post.postId.eq(postBookmark.postId),
                        postBookmark.memberId.eq(memberId)
                )
                .where(
                        post.memberId.eq(memberId),
                        post.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED)
                )
                .orderBy(orderBy(sort))
                .offset(offset)
                .limit(size)
                .fetch();
    }

    @Override
    public List<PostResult> findPosts(Long memberId, PostCategory category, PostSort sort, PostCursor cursor, int size) {
        return queryFactory.select(
                        Projections.constructor(
                                PostResult.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                postLike.postLikeId.isNotNull(),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.location.region,
                                post.publishedAt,
                                Projections.constructor(
                                        PostResult.AuthorResult.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                )
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postLike)
                .on(
                        post.postId.eq(postLike.postId),
                        memberIdEq(postLike.memberId, memberId)
                )
                .leftJoin(postBookmark)
                .on(
                        post.postId.eq(postBookmark.postId),
                        memberIdEq(postBookmark.memberId, memberId)
                )
                .where(
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED),
                        categoryEq(category),
                        cursorCondition(sort, cursor)
                )
                .orderBy(orderBy(sort))
                .limit(size)
                .fetch();
    }

    @Override
    public List<MainFeedPostResult> findMainFeedPosts(Long memberId, PostCategory category, PostSort sort, int limit) {
        return queryFactory.select(
                        Projections.constructor(
                                MainFeedPostResult.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                postLike.postLikeId.isNotNull(),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.location.region,
                                post.publishedAt,
                                Projections.constructor(
                                        MainFeedPostResult.AuthorResult.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                )
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postLike)
                .on(
                        post.postId.eq(postLike.postId),
                        memberIdEq(postLike.memberId, memberId)
                )
                .leftJoin(postBookmark)
                .on(
                        post.postId.eq(postBookmark.postId),
                        memberIdEq(postBookmark.memberId, memberId)
                )
                .where(
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED),
                        categoryEq(category)
                )
                .orderBy(orderBy(sort))
                .limit(limit)
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(PostSort sort) {
        if (sort == PostSort.POPULAR) {
            return new OrderSpecifier[]{
                    post.likeCount.desc(),
                    post.postId.desc()
            };
        }

        return new OrderSpecifier[]{
                post.publishedAt.desc(),
                post.postId.desc()
        };
    }

    private BooleanExpression categoryEq(PostCategory category) {
        if (category == null) {
            return null;
        }

        return post.category.eq(category);
    }

    private BooleanExpression cursorCondition(PostSort sort, PostCursor cursor) {
        if (cursor == null) {
            return null;
        }

        if (sort == PostSort.POPULAR) {
            return post.likeCount.lt(cursor.likeCount())
                    .or(post.likeCount.eq(cursor.likeCount())
                            .and(post.postId.lt(cursor.postId())));
        }

        return post.publishedAt.lt(cursor.publishedAt())
                .or(post.publishedAt.eq(cursor.publishedAt())
                        .and(post.postId.lt(cursor.postId())));
    }

    private BooleanExpression memberIdEq(NumberPath<Long> memberIdPath, Long memberId) {
        if (memberId == null) {
            return Expressions.FALSE;
        }

        return memberIdPath.eq(memberId);
    }

    @Override
    public Optional<PostDetailResult> findPostDetail(Long memberId, Long postId) {
        PostDetailResult result = queryFactory.select(
                        Projections.constructor(
                                PostDetailResult.class,
                                post.postId,
                                post.title,
                                post.content,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                memberId == null ? Expressions.FALSE : post.memberId.eq(memberId),
                                postLike.postLikeId.isNotNull(),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.publishedAt,
                                Projections.constructor(
                                        PostDetailResult.AuthorResult.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                ),
                                Projections.constructor(
                                        PostDetailResult.LocationResult.class,
                                        post.location.region,
                                        post.location.placeName,
                                        post.location.address,
                                        post.location.latitude,
                                        post.location.longitude
                                )
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postLike)
                .on(
                        post.postId.eq(postLike.postId),
                        memberIdEq(postLike.memberId, memberId)
                )
                .leftJoin(postBookmark)
                .on(
                        post.postId.eq(postBookmark.postId),
                        memberIdEq(postBookmark.memberId, memberId)
                )
                .where(
                        post.postId.eq(postId),
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private OrderSpecifier<?>[] orderBy(MyPostSort sort) {
        if (sort == MyPostSort.POPULAR) {
            return new OrderSpecifier[]{
                    post.likeCount.desc(),
                    post.postId.desc()
            };
        }

        return new OrderSpecifier[]{
                post.publishedAt.desc(),
                post.postId.desc()
        };
    }

    @Override
    public List<DraftListResponse> findMyDrafts(Long memberId) {
        return queryFactory.select(
                        Projections.constructor(
                                DraftListResponse.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.updatedAt))
                .from(post)
                .where(post.memberId.eq(memberId),
                        post.status.eq(PostStatus.DRAFT))
                .orderBy(post.updatedAt.desc())
                .fetch();
    }

    @Override
    public Optional<DraftDetailResult> findMyDraftsDetail(Long memberId, Long postId) {
        DraftDetailResult result = queryFactory.select(
                        Projections.constructor(
                                DraftDetailResult.class,
                                post.postId,
                                post.title,
                                post.content,
                                post.category,
                                post.updatedAt,
                                Projections.constructor(
                                        DraftDetailResult.LocationResult.class,
                                        post.location.region,
                                        post.location.placeName,
                                        post.location.address,
                                        post.location.latitude,
                                        post.location.longitude)))
                .from(post)
                .where(post.memberId.eq(memberId),
                        post.postId.eq(postId),
                        post.status.eq(PostStatus.DRAFT))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
