package com.leisure.global.event.publisher;

import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.global.event.dto.EventType;
import com.leisure.global.properties.RabbitMqProperties;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.dto.PointEarnMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("도메인 이벤트 발행자 (DomainEventPublisher)")
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMqProperties properties;

    @InjectMocks
    private DomainEventPublisher publisher;

    private EventEnvelope<PointEarnMessage> envelope(String eventId, EventType type) {
        return EventEnvelope.of(eventId, type, eventId,
                new PointEarnMessage(1L, 2L, 10L, PointType.LIKE_RECEIVED));
    }

    @Test
    @DisplayName("eventType을 'post.liked.pointed' 형태의 라우팅 키로 변환해 도메인 익스체인지로 발행한다")
    void publish_derivesRoutingKeyAndSends() {
        given(properties.domainExchange()).willReturn("domain.events");

        publisher.publish(envelope("evt-1", EventType.POST_LIKED));

        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq("domain.events"), routingKey.capture(), any(Object.class), any(CorrelationData.class));
        assertThat(routingKey.getValue()).isEqualTo("post.liked.pointed");
    }

    @Test
    @DisplayName("envelope이 null이면 IllegalArgumentException")
    void publish_nullEnvelope_throws() {
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("eventId가 비어 있으면 IllegalArgumentException")
    void publish_blankEventId_throws() {
        EventEnvelope<PointEarnMessage> bad = envelope("  ", EventType.POST_LIKED);

        assertThatThrownBy(() -> publisher.publish(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
