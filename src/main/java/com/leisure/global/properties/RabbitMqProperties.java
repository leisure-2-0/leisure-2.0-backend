package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rabbitmq")
public record RabbitMqProperties(
        String domainExchange,

        String deadLetterExchange,

        String apiQueue,

        String apiDlq
) {}
