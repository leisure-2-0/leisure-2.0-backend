package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket,

        String region,

        String publicBaseUrl,

        @DefaultValue("5m") Duration presignExpiration
) {}
