package com.leisure.global.config;

import com.leisure.global.properties.WebClientProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfiguration {

    private final WebClientProperties webClientProperties;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientProperties.connectTimeoutMs())
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(webClientProperties.readTimeoutSeconds())));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(webClientProperties.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
