package com.cancerrisk.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MlClientConfig {

    @Bean
    public RestTemplate mlRestTemplate(RestTemplateBuilder builder, MlClientProperties properties) {
        return builder
                .rootUri(properties.baseUrl())
                .setConnectTimeout(properties.connectTimeout())
                .setReadTimeout(properties.readTimeout())
                .build();
    }
}
