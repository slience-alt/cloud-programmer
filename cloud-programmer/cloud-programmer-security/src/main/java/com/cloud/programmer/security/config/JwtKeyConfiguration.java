package com.cloud.programmer.security.config;

import com.cloud.programmer.security.properties.JwtProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfiguration {

    private final ResourceLoader resourceLoader;

    public JwtKeyConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public RSAPublicKey rsaPublicKey(JwtProperties properties) throws Exception {
        String location = properties.getPublicKeyLocation();

        if (location == null || location.trim().isEmpty()) {
            throw new IllegalStateException(
                    "security.jwt.public-key-location 未配置"
            );
        }

        byte[] keyBytes = readPem(location);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "security.jwt",
            name = "private-key-location"
    )
    public RSAPrivateKey rsaPrivateKey(
            JwtProperties properties) throws Exception {

        byte[] keyBytes = readPem(properties.getPrivateKeyLocation());

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    private byte[] readPem(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "RSA 密钥文件不存在：" + location
            );
        }

        String pem;

        try (InputStream inputStream = resource.getInputStream()) {
            pem = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        String base64 = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(base64);
    }
}