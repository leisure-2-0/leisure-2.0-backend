package com.leisure.post.repository;

import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostDetailResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
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

    private final JPAQueryFactory factory;

    @Override
    public List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size) {
        return factory.select(
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

    @Override
    public Optional<PostDetailResponse> findPostDetail(Long memberId, Long postId) {
        PostDetailResponse response = factory.select(
                        Projections.constructor(
                                PostDetailResponse.class,
                                post.postId,
                                post.title,
                                post.content,
                                post.category,
                                post.viewCount,
                                post.likeCount,
                                post.bookmarkCount,
                                post.memberId.eq(memberId),
                                postLike.postLikeId.isNotNull(),
                                postBookmark.postBookmarkId.isNotNull(),
                                post.publishedAt,
                                Projections.constructor(
                                        PostDetailResponse.AuthorResponse.class,
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
                        post.postId.eq(postId),
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.status.eq(PostStatus.PUBLISHED)
                )
                .fetchOne();

        return Optional.ofNullable(response);
    }

    private OrderSpecifier<?>[] orderBy(MyPostSort sort) {
        if (sort == MyPostSort.POPULAR) {
            return new OrderSpecifier[] {
                post.likeCount.desc(),
                post.postId.desc()
            };
        }

        return new OrderSpecifier[] {
                post.publishedAt.desc(),
                post.postId.desc()
        };
    }
}
