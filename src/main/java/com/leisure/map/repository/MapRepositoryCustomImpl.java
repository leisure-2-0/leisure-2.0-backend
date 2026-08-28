package com.leisure.map.repository;

import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.leisure.member.domain.QMember.member;
import static com.leisure.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class MapRepositoryCustomImpl implements MapRepositoryCustom {

    private static final int MAP_PINS_LIMIT = 500;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RegionPinCountResponse> findRegionPinCounts(PostCategory category) {
        return queryFactory.select(
                        Projections.constructor(
                                RegionPinCountResponse.class,
                                post.location.region,
                                post.postId.count(),
                                post.location.latitude.avg(),
                                post.location.longitude.avg()
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .where(
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.location.region.isNotNull(),
                        post.location.latitude.isNotNull(),
                        post.location.longitude.isNotNull(),
                        post.status.eq(PostStatus.PUBLISHED),
                        categoryEq(category)
                )
                // TODO: GROUP BY 컬럼에 인덱스 생성
                .groupBy(post.location.region)
                .fetch();
    }

    @Override
    public List<MapPinResponse> findPinsInBounds(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {
        return queryFactory.select(
                        Projections.constructor(
                                MapPinResponse.class,
                                post.postId,
                                post.title,
                                post.category,
                                post.location.latitude,
                                post.location.longitude
                        )
                )
                .from(post)
                .join(member)
                .on(post.memberId.eq(member.memberId))
                .where(
                        post.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        post.location.latitude.isNotNull(),
                        post.location.longitude.isNotNull(),
                        post.location.latitude.goe(minLat),
                        post.location.latitude.loe(maxLat),
                        post.location.longitude.goe(minLng),
                        post.location.longitude.loe(maxLng),
                        post.status.eq(PostStatus.PUBLISHED),
                        categoryEq(category)
                )
                .orderBy(post.publishedAt.desc(), post.postId.desc())
                .limit(MAP_PINS_LIMIT)
                .fetch();
    }

    private BooleanExpression categoryEq(PostCategory category) {
        if (category == null) {
            return null;
        }

        return post.category.eq(category);
    }
}
