package com.cloud.programmer.security.config;

import com.cloud.programmer.security.jwt.CloudProgrammerJwtAuthenticationConverter;
import com.cloud.programmer.security.properties.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPublicKey;

@Configuration(proxyBeanMethods = false)
public class JwtSecurityConfiguration {

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey, JwtProperties properties) {

        if (properties.getIssuer() == null || properties.getIssuer().trim().isEmpty()) {
            throw new IllegalStateException("security.jwt.issuer 未配置");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(properties.getIssuer());

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken>
    jwtAuthenticationConverter(JwtProperties properties) {
        return new CloudProgrammerJwtAuthenticationConverter(properties);
    }
}