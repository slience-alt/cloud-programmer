package com.cloud.programmer.runtime.client;

import com.cloud.programmer.security.properties.InternalTokenProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AdminClientConfig {

    @Bean("adminRestClient")
    public RestClient adminRestClient(
            RestClient.Builder builder,
            @Value("${admin.url}") String baseUrl,
            InternalTokenProperties internalTokenProperties) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(
                        "X-Internal-Token",
                        internalTokenProperties.getToken()
                )
                .build();
    }
}