package com.leisure.post.repository;

import com.leisure.post.domain.*;
import com.leisure.post.dto.response.MainFeedPostResponse;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.PostDetailResult;
import com.leisure.post.dto.result.PostPinResult;
import com.leisure.post.dto.result.RegionPinCountResult;
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

    // MyPost 조회 ==============================================================
    @Override
    public List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size) {
        return queryFactory.select(
                        Projections.constructor(
                                MyPostResponse.class,
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
                                        MyPostResponse.AuthorResponse.class,
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

    // Post 조회 ==============================================================
    @Override
    public List<PostResponse> findPosts(Long memberId, PostCategory category, PostSort sort, PostCursor cursor, int size) {
        return queryFactory.select(
                        Projections.constructor(
                                PostResponse.class,
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
                                        PostResponse.AuthorResponse.class,
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

    // MainFeed 조회 ==============================================================
    @Override
    public List<MainFeedPostResponse> findMainFeedPosts(Long memberId, PostCategory category, PostSort sort, int limit) {
        return queryFactory.select(
                        Projections.constructor(
                                MainFeedPostResponse.class,
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
                                        MainFeedPostResponse.AuthorResponse.class,
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

    // PostDetail 조회 ==============================================================
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

    // ㄷㅂ? ==============================================================
    private BooleanExpression categoryEq(PostCategory category) {
        if (category == null) {
            return null;
        }

        return post.category.eq(category);
    }

    // Post 조회 시 커서 조건 생성 ==============================================================
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

    // ?
    private BooleanExpression memberIdEq(NumberPath<Long> memberIdPath, Long memberId) {
        if (memberId == null) {
            return Expressions.FALSE;
        }

        return memberIdPath.eq(memberId);
    }

    // PostDetail 조회 ==============================================================
    @Override
    public Optional<PostDetailResult> findPostDetail(Long memberId, Long postId) {
        PostDetailResult response = queryFactory.select(
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

        return Optional.ofNullable(response);
    }

    // MyPost 조회 ==============================================================
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

    

    // 지역별 게시글 개수 조회 ==============================================================
    @Override
    public List<RegionPinCountResult> findRegionPinCounts(PostCategory category) {
        return queryFactory.select(
                        Projections.constructor(
                                RegionPinCountResult.class,
                                post.location.region,
                                post.postId.count(),
                                post.location.latitude.avg(),
                                post.location.longitude.avg()
                        )
                )
                .from(post)
                .where(
                        post.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED),
                        post.location.region.isNotNull(),
                        categoryEq(category)
                )
                .groupBy(post.location.region)
                .fetch();
    }


    // 지도 범위 내 게시글 핀 조회 ==============================================================
    private static final int MAP_PINS_LIMIT = 500; // 지도 범위 내 게시글 핀 조회 시 최대 500개까지만 조회하도록 제한
    
    @Override
    public List<PostPinResult> findPinsInBounds(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {
        return queryFactory.select(
                        Projections.constructor(
                                PostPinResult.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.location.latitude,
                                post.location.longitude
                        )
                )
                .from(post)
                .where(
                        post.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED),
                        post.location.latitude.isNotNull(),
                        post.location.longitude.isNotNull(),
                        post.location.latitude.goe(minLat),
                        post.location.latitude.loe(maxLat),
                        post.location.longitude.goe(minLng),
                        post.location.longitude.loe(maxLng),
                        categoryEq(category)
                )
                .limit(MAP_PINS_LIMIT)
                .fetch();
    }
}
