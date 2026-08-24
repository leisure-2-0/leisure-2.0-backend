package com.leisure.postLike.repository;

import com.leisure.post.domain.PostStatus;
import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.response.LikedPostResponse;
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
public class PostLikeCustomImpl implements PostLikeCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LikedPostResponse> findLikedPosts(Long memberId, LikedPostSort sort, long offset, int size) {
        return queryFactory
                .select(
                        Projections.constructor(
                                LikedPostResponse.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                post.memberId.eq(memberId),
                                postLike.memberId.eq(memberId),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.publishedAt,
                                postLike.createdAt,
                                Projections.constructor(
                                        LikedPostResponse.AuthorResponse.class,
                                        member.memberId,
                                        member.nickname,
                                        member.profileImageUrl
                                )
                        )
                )
                .from(postLike)
                .join(post)
                .on(postLike.postId.eq(post.postId))
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .leftJoin(postBookmark)
                .on(
                        postBookmark.postId.eq(post.postId),
                        postBookmark.memberId.eq(memberId)
                )
                .where(
                        postLike.memberId.eq(memberId),
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED)
                )
                .orderBy(orderBy(sort))
                .offset(offset)
                .limit(size)
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(LikedPostSort sort) {
        if (sort == LikedPostSort.POPULAR) {
            return new OrderSpecifier[] {
                    post.likeCount.desc(),
                    post.postId.desc()
            };
        }

        return  new OrderSpecifier[]{
                postLike.createdAt.desc(),
                postLike.postLikeId.desc()
        };
    }
}
