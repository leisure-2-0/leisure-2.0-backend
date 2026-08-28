package com.leisure.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("swagger")
public record SwaggerProperties(String title, String description, String version, Map<String, String> servers) {}
