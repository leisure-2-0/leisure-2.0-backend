package com.leisure.image.scheduler;

import com.leisure.global.properties.S3Properties;
import com.leisure.member.repository.MemberRepository;
import com.leisure.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image.cleanup.enabled", havingValue = "true")
public class ImageCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanupScheduler.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void cleanupOrphans() {
        log.info("[image-cleanup] 고아 이미지 청소 시작");

        try {
            String base = s3Properties.publicBaseUrl() + "/";

            Set<String> referencedKeys = new HashSet<>();
            Stream.concat(
                    postRepository.findAllThumbnailUrls().stream(),
                    memberRepository.findAllProfileImageUrls().stream()
            ).forEach(url -> {
                if (url != null && url.startsWith(base)) {
                    referencedKeys.add(url.substring(base.length()));
                }
            });

            Pattern pattern = Pattern.compile(Pattern.quote(base) + "([^\"'\\s)]+)");
            for (String content : postRepository.findAllContents()) {
                if (content == null) {
                    continue;
                }
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    referencedKeys.add(matcher.group(1));
                }
            }

            Instant threshold = Instant.now().minus(Duration.ofHours(48));

            int deleted = 0;

            for (String prefix : List.of("thumbnails/", "profiles/", "contents/")) {
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(s3Properties.bucket())
                        .prefix(prefix)
                        .build();

                for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(request)) {
                    for (S3Object obj : page.contents()) {
                        if (!referencedKeys.contains(obj.key()) && obj.lastModified().isBefore(threshold)) {
                            s3Client.deleteObject(b -> b.bucket(s3Properties.bucket()).key(obj.key()));
                            deleted++;
                        }
                    }
                }
            }

            log.info("[image-cleanup] 완료 - 삭제 {}건", deleted);
        } catch (Exception e) {
            log.error("[image-cleanup] 청소 실패 - 다음 스케줄에 재시도", e);
        }
    }
}
