package com.leisure.festival.repository;

import com.leisure.festival.dto.result.DailyFestivalResult;
import com.leisure.festival.dto.result.UpcomingFestivalResult;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.leisure.festival.domain.QFestival.festival;
import static com.leisure.region.domain.QRegion.region;


@Repository
@RequiredArgsConstructor
public class FestivalRepositoryCustomImpl implements FestivalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DailyFestivalResult> findDailyFestivals(LocalDate date, String code) {
        return queryFactory.select(Projections.constructor(
                                DailyFestivalResult.class,
                                festival.lclsSystm2,
                                region.signguName,
                                festival.name,
                                festival.overview,
                                festival.eventTime,
                                festival.homepageUrl
                        )
                )
                .from(festival)
                .leftJoin(region)
                .on(
                        festival.ldongRegnCd.eq(region.ldongRegnCd),
                        festival.ldongSignguCd.eq(region.ldongSignguCd)
                )
                .where(
                        festival.eventStartDate.loe(date),
                        festival.eventEndDate.goe(date),
                        code != null ? festival.lclsSystm2.eq(code) : null
                )
                .orderBy(festival.name.asc())
                .fetch();
    }

    @Override
    public List<UpcomingFestivalResult> findUpcomingFestivals(LocalDate today) {
        return queryFactory.select(Projections.constructor(
                        UpcomingFestivalResult.class,
                            festival.festivalId,
                            festival.name,
                            region.signguName,
                            festival.eventStartDate,
                            festival.thumbnailUrl
                        )
                )
                .from(festival)
                .leftJoin(region)
                .on(
                        festival.ldongRegnCd.eq(region.ldongRegnCd),
                        festival.ldongSignguCd.eq(region.ldongSignguCd)
                )
                .where(festival.eventStartDate.gt(today))
                .orderBy(
                        festival.eventStartDate.asc(),
                        festival.festivalId.asc()
                )
                .limit(10)
                .fetch();
    }
}
