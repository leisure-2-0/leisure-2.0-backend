package com.leisure.search.index;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostIndexScheduler {

    private static final Logger log = LoggerFactory.getLogger(PostIndexScheduler.class);

    private final PostIndexService postIndexService;

    @Scheduled(fixedDelayString = "${search.sync.fixed-delay-ms}")
    public void syncChangedPosts() {

        try {
            postIndexService.syncChangedPosts();
        } catch (Exception e) {
            log.error("[post-index] 스케줄 색인 중 예기치 못한 오류", e);
        }
    }
}
