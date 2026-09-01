package com.leisure.region.scheduler;

import com.leisure.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RegionScheduler.class);

    private final RegionService service;

    @Scheduled(cron = "0 0 4 1 1,4,7,10 *", zone = "Asia/Seoul")
    public void syncRegions() {
        log.info("[region-sync] 지역 배치 시작");

        try {
            service.syncRegions();
        } catch (Exception e) {
            log.error("[region-sync] 지역 배치 실패 - 다음 스케줄에 재시도", e);
        }
    }
}
