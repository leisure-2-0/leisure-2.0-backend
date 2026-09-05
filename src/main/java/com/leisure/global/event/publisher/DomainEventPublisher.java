package com.leisure.global.event.publisher;

import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.global.event.dto.EventType;
import com.leisure.global.properties.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// [레거시] publishAfterCommit 주석 처리로 미사용 (도메인 이벤트 리스너 방식으로 대체)
//import org.springframework.transaction.support.TransactionSynchronization;
//import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    private final RabbitMqProperties properties;

    public void publish(EventEnvelope<?> envelope) {

        if (envelope == null) {
            throw new IllegalArgumentException("발행할 이벤트 봉투(envelope)가 null입니다.");
        }

        if (!StringUtils.hasText(envelope.eventId())) {
            throw new IllegalArgumentException("이벤트 봉투의 eventId가 비어 있습니다.");
        }

        String routingKey = from(envelope.eventType(), ".pointed");

        CorrelationData correlationData = new CorrelationData(envelope.eventId());

        rabbitTemplate.convertAndSend(properties.domainExchange(), routingKey, envelope, correlationData);

        correlationData.getFuture()
                .orTimeout(10L, TimeUnit.SECONDS)
                .whenComplete((confirm, exception) -> {

                    if (exception != null) {
                        if (exception instanceof TimeoutException) {
                            log.error("[point-publisher] 발행 확인 타임아웃(브로커 무응답/지연) eventId={} eventType={} routingKey={}",
                                    envelope.eventId(), envelope.eventType(), routingKey);
                        } else {
                            log.error("[point-publisher] 발행 확인 대기 중 예외 eventId={} eventType={} routingKey={}",
                                    envelope.eventId(), envelope.eventType(), routingKey, exception);
                        }
                        return;
                    }

                    if (confirm == null || !confirm.ack()) {
                        log.error("[point-publisher] 발행 미확인(nack) eventId={} eventType={} routingKey={} reason={}",
                                envelope.eventId(), envelope.eventType(), routingKey,
                                confirm != null ? confirm.reason() : null);
                        return;
                    }

                    log.debug("[point-publisher] 발행 완료 eventId={} eventType={} correlationId={} routingKey={}",
                            envelope.eventId(), envelope.eventType(), envelope.correlationId(), routingKey);
                });
    }

    private static String from(EventType eventType, String suffix) {
        if (eventType == null) {
            throw new IllegalArgumentException("이벤트 타입(eventType)이 null입니다.");
        }
        return eventType.name().toLowerCase().replace("_", ".") + suffix;
    }

//    public void publishAfterCommit(EventEnvelope<?> envelope) {
//        if (!isActualTransactionActive() || !isSynchronizationActive()) {
//            publish(envelope);
//            return;
//        }
//
//        registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                publish(envelope);
//            }
//        });
//    }
}
