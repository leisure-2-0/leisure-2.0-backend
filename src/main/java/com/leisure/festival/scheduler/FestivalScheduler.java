package com.leisure.festival.scheduler;

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

    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void syncFestivalList() {
        log.info("[festival-sync] 축제 목록 배치 시작");

        try {
            service.syncFestivalList();
        } catch (Exception e) {
            log.error("[festival-sync] 목록 배치 실패 - 다음 스케줄에 재시도", e);
        }
    }

    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    public void syncOverviewAndHomepage() {
        log.info("[festival-detail] 소개글·홈페이지 배치 시작");

        try {
            service.syncOverviewAndHomepage();
        } catch (Exception e) {
            log.error("[festival-detail] 소개글, 홈페이지 배치 실패 - 다음 스케줄에 재시도", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void syncEventTime() {
        log.info("[festival-detail] 운영시간 배치 시작");

        try {
            service.syncEventTime();
        } catch (Exception e) {
            log.error("[festival-detail] 운영시간 배치 실패 - 다음 스케줄에 재시도", e);
        }
    }
}
