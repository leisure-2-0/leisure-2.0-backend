package com.leisure.global.config;

import com.leisure.global.properties.RabbitMqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    private final RabbitMqProperties properties;

    public RabbitMqConfiguration(RabbitMqProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jacksonJsonMessageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonJsonMessageConverter);

        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {

            if (ack) {
                return;
            }

            log.error("[rabbit] 발행 미확인 correlation Id={} cause={}", correlation != null ? correlation.getId() : null, cause);
        });

        rabbitTemplate.setReturnsCallback(returned ->
                log.error("[rabbit] 라우팅 실패 반환 exchange={} routingKey={} replyCode={} reason={} messageId={}",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText(),
                        returned.getMessage().getMessageProperties().getMessageId()));

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jacksonJsonMessageConverter) {

        SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setConnectionFactory(connectionFactory);
        containerFactory.setMessageConverter(jacksonJsonMessageConverter);
        containerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        containerFactory.setPrefetchCount(10); // 공식 있? 있으면 인라인 주석으로 만들어줘
        containerFactory.setDefaultRequeueRejected(false);
        containerFactory.setErrorHandler(new ConditionalRejectingErrorHandler());

        return containerFactory;
    }

    @Bean
    public TopicExchange domainExchange() {
        return ExchangeBuilder
                .topicExchange(properties.domainExchange())
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder
                .topicExchange(properties.deadLetterExchange())
                .durable(true)
                .build();
    }

    @Bean
    public Queue apiQueue() {
        return QueueBuilder
                .durable(properties.apiQueue())
                .deadLetterExchange(properties.deadLetterExchange())
                .build();
    }

    @Bean
    public Queue apiDlq() {
        return QueueBuilder
                .durable(properties.apiDlq())
                .build();
    }

    @Bean
    public Binding pointEventBinding(Queue apiQueue, TopicExchange domainExchange) {
        return BindingBuilder
                .bind(apiQueue)
                .to(domainExchange)
                .with("#.pointed");
    }

    @Bean
    public Binding apiDlqBinding(Queue apiDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder
                .bind(apiDlq)
                .to(deadLetterExchange)
                .with("#");
    }
}
