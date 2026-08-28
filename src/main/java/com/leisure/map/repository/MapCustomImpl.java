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

import static com.leisure.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class MapCustomImpl implements MapCustom {

    private final JPAQueryFactory queryFactory;

    private static final int MAP_PINS_LIMIT = 500; // 지도 범위 내 게시글 핀 조회 시 최대 500개까지만 조회하도록 제한

    // 지역별 게시글 개수 조회 ==============================================================
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

    // map에서 처음 필요해진 카테고리 필터 헬퍼 _ post 쪽 PostCustomImpl.categoryEq와 동일한 로직(재사용 불가라 복붙)
    private BooleanExpression categoryEq(PostCategory category) {
        if (category == null) {
            return null;
        }

        return post.category.eq(category);
    }
}
