package com.leisure.global.config;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfiguration {

    private final SwaggerProperties properties;

    public SwaggerConfiguration(SwaggerProperties properties) {
        this.properties = properties;
    }

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMember.class);
    }

    @Bean
    public OpenAPI openAPI() {

        String localServerUrl = properties.servers().getOrDefault("local", "http://localhost:8080");

        return new OpenAPI()
                .info(new Info()
                        .title(properties.title())
                        .description(properties.description())
                        .version(properties.version()))
                .servers(List.of(
                        new Server().url(localServerUrl).description("로컬 테스트용")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                    new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .description("로그인 응답으로 받은 Access Token을 입력하세요. Swagger UI가 Authorization 헤더에 Bearer 토큰으로 전송합니다.")));
    }
}
