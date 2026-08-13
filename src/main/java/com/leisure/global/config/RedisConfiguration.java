package com.leisure.global.config;

import com.leisure.global.auth.properties.RedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfiguration {

    private final RedisProperties properties;

    public RedisConfiguration(RedisProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(properties.host(), properties.port());
        configuration.setDatabase(properties.database());

//        if (password != null && !password.isBlank()) {
//            configuration.setPassword(password);
//        }

        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}


//@Value("${spring.data.redis.host}")
//private final String host;
//
//@Value("${spring.data.redis.port}")
//private final int port;

//private final String host;
//private final int port;
//
//public RedisConfiguration(
//        @Value("${spring.data.redis.host}") String host,
//        @Value("${spring.data.redis.port}") int port) {
//    this.host = host;
//    this.port = port;
//}