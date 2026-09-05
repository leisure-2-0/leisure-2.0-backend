package com.leisure.pointhistory.listener;

import com.leisure.bookmark.event.PostBookmarkedEvent;
import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.global.event.dto.EventType;
import com.leisure.global.event.publisher.DomainEventPublisher;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.dto.PointEarnMessage;
import com.leisure.post.event.PostPublishedEvent;
import com.leisure.postlike.event.PostLikedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 적립 이벤트 리스너 (PointEarnEventListener)")
class PointEarnEventListenerTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private PointEarnEventListener listener;

    private static final Long AUTHOR = 1L;   // 작성자(수령자)
    private static final Long ACTOR = 2L;    // 행위자(타인)
    private static final Long POST_ID = 10L;

    @Test
    @DisplayName("게시: 작성자 본인이라도 POST_PUBLISH 봉투를 발행한다(self 제외 없음)")
    void onPostPublished_alwaysPublishes() {
        listener.onPostPublished(new PostPublishedEvent(AUTHOR, POST_ID));

        PointEarnMessage msg = capturePayload();
        assertThat(msg.memberId()).isEqualTo(AUTHOR);
        assertThat(msg.actorId()).isEqualTo(AUTHOR);   // 게시는 actor=author
        assertThat(msg.sourceId()).isEqualTo(POST_ID);
        assertThat(msg.pointType()).isEqualTo(PointType.POST_PUBLISH);
    }

    @Test
    @DisplayName("좋아요(타인): POST_LIKED 봉투를 발행하고 필드를 정확히 매핑한다")
    void onPostLiked_otherActor_publishes() {
        listener.onPostLiked(new PostLikedEvent(AUTHOR, ACTOR, POST_ID));

        EventEnvelope<?> envelope = captureEnvelope();
        assertThat(envelope.eventType()).isEqualTo(EventType.POST_LIKED);

        PointEarnMessage msg = (PointEarnMessage) envelope.data();
        assertThat(msg.memberId()).isEqualTo(AUTHOR);
        assertThat(msg.actorId()).isEqualTo(ACTOR);
        assertThat(msg.sourceId()).isEqualTo(POST_ID);
        assertThat(msg.pointType()).isEqualTo(PointType.LIKE_RECEIVED);
    }

    @Test
    @DisplayName("좋아요(본인): self 제외로 발행하지 않는다")
    void onPostLiked_selfActor_skips() {
        listener.onPostLiked(new PostLikedEvent(AUTHOR, AUTHOR, POST_ID));

        verify(domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("북마크(타인): POST_BOOKMARKED 봉투를 발행한다")
    void onPostBookmarked_otherActor_publishes() {
        listener.onPostBookmarked(new PostBookmarkedEvent(AUTHOR, ACTOR, POST_ID));

        EventEnvelope<?> envelope = captureEnvelope();
        assertThat(envelope.eventType()).isEqualTo(EventType.POST_BOOKMARKED);

        PointEarnMessage msg = (PointEarnMessage) envelope.data();
        assertThat(msg.pointType()).isEqualTo(PointType.BOOKMARK_RECEIVED);
    }

    @Test
    @DisplayName("북마크(본인): self 제외로 발행하지 않는다")
    void onPostBookmarked_selfActor_skips() {
        listener.onPostBookmarked(new PostBookmarkedEvent(AUTHOR, AUTHOR, POST_ID));

        verify(domainEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("봉투에 eventId가 채워지고 correlationId로 재사용된다")
    void envelope_hasEventIdReusedAsCorrelationId() {
        listener.onPostPublished(new PostPublishedEvent(AUTHOR, POST_ID));

        EventEnvelope<?> envelope = captureEnvelope();
        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.correlationId()).isEqualTo(envelope.eventId());
    }

    private EventEnvelope<?> captureEnvelope() {
        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(domainEventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private PointEarnMessage capturePayload() {
        return (PointEarnMessage) captureEnvelope().data();
    }
}
