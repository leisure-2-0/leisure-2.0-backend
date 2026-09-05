package com.leisure.festival.service;

import com.leisure.festival.domain.Festival;
import com.leisure.festival.dto.command.FestivalData;
import com.leisure.festival.dto.result.FestivalSyncResult;
import com.leisure.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Component
@RequiredArgsConstructor
public class FestivalWriter {

    private final FestivalRepository repository;

    @Transactional
    public FestivalSyncResult updates(List<FestivalData> list) {

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

        return new FestivalSyncResult(inserted, updated, (inserted + updated));
    }

    @Transactional
    public void updateDetailCommon(String tourContentId, String overview, String homepageUrl) {
        repository.findByTourContentId(tourContentId)
                .ifPresent(festival -> festival.updateFromDetailCommon(overview, homepageUrl));
    }

    @Transactional
    public void updateDetailIntro(String tourContentId, String eventTime) {
        repository.findByTourContentId(tourContentId)
                .ifPresent(festival -> festival.updateFromDetailIntro(eventTime));
    }
}
