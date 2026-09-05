package com.leisure.pointhistory.consumer;

import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.global.event.dto.EventType;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.dto.PointEarnMessage;
import com.leisure.pointhistory.service.PointHistoryService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 적립 컨슈머 (PointEarnConsumer)")
class PointEarnConsumerTest {

    @Mock
    private PointHistoryService service;

    @Mock
    private Channel channel;

    @InjectMocks
    private PointEarnConsumer consumer;

    private static final long DELIVERY_TAG = 5L;

    private Message messageWithTag() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        return new Message(new byte[0], props);
    }

    private EventEnvelope<PointEarnMessage> envelope() {
        return EventEnvelope.of("evt-1", EventType.POST_LIKED, "evt-1",
                new PointEarnMessage(1L, 2L, 10L, PointType.LIKE_RECEIVED));
    }

    @Test
    @DisplayName("정상 처리 시 봉투를 언래핑해 earn 호출 후 basicAck 한다")
    void onMessage_success_acks() throws Exception {
        consumer.onMessage(envelope(), messageWithTag(), channel);

        verify(service).earn(1L, 2L, 10L, PointType.LIKE_RECEIVED);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("처리 실패 시 basicNack(requeue=false)로 DLQ로 보낸다")
    void onMessage_failure_nacksToDlq() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(service).earn(any(), any(), any(), any());

        consumer.onMessage(envelope(), messageWithTag(), channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, org.mockito.Mockito.never()).basicAck(eq(DELIVERY_TAG), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
