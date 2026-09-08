package com.leisure.search.index;

import com.leisure.global.properties.SearchProperties;
import com.leisure.post.domain.Post;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.engine.PostSearchDocument;
import com.leisure.search.engine.PostSearchRepository;
import com.leisure.tag.service.TagReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostIndexService {

    private static final Logger log = LoggerFactory.getLogger(PostIndexService.class);

    private final SearchProperties searchProperties;

    private final PostIndexDirtyRepository postIndexDirtyRepository;

    private final PostRepository postRepository;

    private final TagReader tagReader;

    private final PostSearchRepository postSearchRepository;


    /**
     * 증분 색인 — 더티 큐에 쌓인 "바뀐 글"만 ES에 반영한다. (스케줄러가 주기적으로 호출)
     */
    public void syncChangedPosts() {

        if (!searchProperties.enabled()) {
            return;   // 검색 기능 자체가 꺼져 있으면 아무것도 안 한다
        }

        // 현재는 단일 인스턴스 전제. 멀티 팟(k8s 등)이면 여러 팟이 findProcessable로 같은 더티 행을
        // 동시에 잡아 중복 색인, 불필요한 ES 부하가 생길 수 있다(경합).
        // version 가드 + id 기준 멱등 upsert 덕에 데이터 손상은 없지만, "중복 작업"이 문제.
        // 다중 인스턴스로 확장하면 DB 레벨 저수준 제어가 필요: 행 클레임(SELECT ... FOR UPDATE SKIP LOCKED)로
        // 각 팟이 서로 다른 배치만 처리하거나, 분산 락(ShedLock 등)으로 한 팟만 실행되게 한다.
        List<PostIndexDirty> dirties = postIndexDirtyRepository.findProcessable(LocalDateTime.now(), searchProperties.sync().batchSize());
        if (dirties.isEmpty()) {
            return;
        }

        // 1) 더티 글을 "넣을 것(공개) / 뺄 것(삭제, 비공개)" 두 바구니로 나눈다
        List<PostSearchDocument> documents = new ArrayList<>();
        List<Long> deletedPosts = new ArrayList<>();

        for (PostIndexDirty dirty : dirties) {
            Long postId = dirty.getPostId();
            Optional<Post> found = postRepository.findByPostIdAndDeletedAtIsNull(postId);

            if (found.isPresent() && found.get().getStatus() == PostStatus.PUBLISHED) {
                documents.add(toDocument(found.get()));
            } else {
                deletedPosts.add(postId);   // 없음(소프트삭제) 또는 비공개 → ES에서 제거
            }
        }

        // 2) ES 반영은 한 번씩(bulk). 성공하면 version 가드로 더티 제거, 실패하면 백오프
        try {
            postSearchRepository.index(documents);
            postSearchRepository.delete(deletedPosts);

            // 처리하는 사이 그 글이 또 수정됐으면(version 변경) 0건 삭제 → 더티 남아 다음 회차 재처리
            for (PostIndexDirty dirty : dirties) {
                postIndexDirtyRepository.deleteIfVersionMatches(dirty.getPostId(), dirty.getVersion());
            }

            log.debug("[post-index] 색인 동기화 완료 — 색인 {}건, 삭제 {}건", documents.size(), deletedPosts.size());
        } catch (RuntimeException e) {
            log.warn("[post-index] 색인 동기화 실패 — 더티 {}건 백오프 후 재시도", dirties.size(), e);

            for (PostIndexDirty dirty : dirties) {
                postIndexDirtyRepository.markFailure(dirty.getPostId(), dirty.getVersion(), nextRetryAt(dirty.getRetryCount()));
            }
        }
    }

    /**
     * 최초 전체 색인 — 앱 시작 시 인덱스(그릇)를 만들고, 플래그가 켜져 있으면 기존 공개글을 전부 백필한다.
     * ES 장애가 있어도 기동이 죽지 않도록 예외를 잡아 로그만 남긴다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void indexExistingPosts() {

        if (!searchProperties.enabled()) {
            return;   // 검색 기능 자체가 꺼져 있으면 인덱스 생성/백필 안 함
        }

        Long cursor = 0L;
        int count = 0;
        try {
            postSearchRepository.initIndex();   // 인덱스(그릇) 없으면 생성 - 매 기동, 멱등

            if (!searchProperties.initialIndex().enabled()) {
                return;   // 전체 백필은 initial-index 플래그가 켜졌을 때만
            }

            // 커서(postId 키셋)로 공개글을 청크씩 읽어 바로 ES에 색인 (메모리 bound)
            while (true) {
                List<Post> posts = postRepository.findPostWithCursor(searchProperties.sync().batchSize(), cursor);
                if (posts.isEmpty()) {
                    break;
                }

                List<PostSearchDocument> documents = posts.stream().map(this::toDocument).toList();
                postSearchRepository.index(documents);

                count += documents.size();
                cursor = posts.get(posts.size() - 1).getPostId();   // 다음 커서 = 이번 청크 마지막 postId
            }

            log.info("[post-index] 전체 색인 완료 — {}건", count);
        } catch (RuntimeException e) {
            log.error("[post-index] 초기 색인 실패 — {}건까지 처리 후 중단(기동은 계속)", count, e);
        }
    }

    /** 글 + 태그 → ES 문서. category/location은 없을 수 있어 null 방어. */
    private PostSearchDocument toDocument(Post post) {
        String category = post.getCategory() != null ? post.getCategory().name() : null;
        String region = post.getLocation() != null ? post.getLocation().getRegion() : null;

        return PostSearchDocument.of(
                post.getPostId(),
                post.getTitle(),
                tagReader.findTags(post.getPostId()),
                category,
                region);
    }

    /** 지수 백오프: 실패 횟수가 늘수록 다음 재시도를 뒤로 민다(상한 있음). */
    private LocalDateTime nextRetryAt(int retryCount) {
        long delay = Math.min(30 * (1L << Math.min(retryCount, 20)), 600);
        return LocalDateTime.now().plusSeconds(delay);
    }
}
