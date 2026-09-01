package com.leisure.festival.service;

import com.leisure.festival.domain.Festival;
import com.leisure.festival.dto.FestivalData;
import com.leisure.festival.dto.result.FestivalSyncResult;
import com.leisure.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Component
@RequiredArgsConstructor
public class FestivalUpserter {

    private static final Logger log = LoggerFactory.getLogger(FestivalUpserter.class);

    private final FestivalRepository repository;

    @Transactional
    public FestivalSyncResult upsert(List<FestivalData> list) {

        int inserted = 0, updated = 0;

        for (FestivalData festivalData : list) {
            Festival festival = repository.findByTourContentId(festivalData.tourContentId()).orElse(null);

            if (festival == null) {
                repository.save(Festival.create(festivalData));
                inserted++;
            } else {
                festival.updateFromList(festivalData);
                updated++;
            }
        }

        log.info("[festival-sync] 완료 inserted={}, updated={}", inserted, updated);

        return new FestivalSyncResult(inserted, updated, (inserted + updated));
    }
}
