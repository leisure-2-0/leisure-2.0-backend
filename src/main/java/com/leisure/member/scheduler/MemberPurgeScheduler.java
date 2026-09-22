package com.leisure.member.scheduler;

import com.leisure.member.service.MemberPurgeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "member.purge.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(MemberPurgeScheduler.class);

    private final MemberPurgeService memberPurgeService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purge() {
        log.info("[member-purge] 탈퇴 회원 배치 시작");

        try {
            int purged = memberPurgeService.purgeWithdrawnMembers();
            log.info("[member-purge] 완료 - {}명 배치", purged);
        } catch (Exception e) {
            log.error("[member-purge] 실패 - 다음 스케줄에 재시도", e);
        }
    }
}
