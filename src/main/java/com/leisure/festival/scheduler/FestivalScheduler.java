package com.leisure.festival.scheduler;

import com.leisure.festival.dto.result.FestivalSyncResult;
import com.leisure.festival.service.FestivalService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalScheduler {

    private static final Logger log = LoggerFactory.getLogger(FestivalScheduler.class);

    private final FestivalService service;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void syncFestivals() {
        log.info("[festival-sync] 축제 목록 배치 시작");
        try {
            FestivalSyncResult result = service.syncFestivals();
            log.info("[festival-sync] 배치 완료 inserted={}, updated={}, total={}",
                    result.inserted(), result.updated(), result.total());
        } catch (Exception e) {
            log.error("[festival-sync] 배치 실패 — 다음 스케줄에 재시도", e);
        }
    }
}
