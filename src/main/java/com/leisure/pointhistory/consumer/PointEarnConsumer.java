package com.leisure.pointhistory.consumer;

import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.pointhistory.dto.PointEarnMessage;
import com.leisure.pointhistory.service.PointHistoryService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PointEarnConsumer {

    private static final Logger log = LoggerFactory.getLogger(PointEarnConsumer.class);

    private final PointHistoryService service;

    @RabbitListener(queues = "${rabbitmq.api-queue}", containerFactory = "simpleRabbitListenerContainerFactory")
    public void onMessage(EventEnvelope<PointEarnMessage> envelope, Message message, Channel channel) throws IOException {

        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();

        try {
            PointEarnMessage msg = envelope.data();

            log.debug("[point-consumer] 수신 eventId={} correlationId={} eventType={} pointType={}",
                    envelope.eventId(), envelope.correlationId(), envelope.eventType(), msg.pointType());

            service.earn(msg.memberId(), msg.actorId(), msg.sourceId(), msg.pointType());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[point-consumer] 실패 → DLQ eventId={} correlationId={} redelivered={} tag={}",
                    envelope.eventId(), envelope.correlationId(), messageProperties.isRedelivered(), deliveryTag, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
