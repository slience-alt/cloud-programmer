
package com.cloud.programmer.runtime.config;

import com.cloud.programmer.security.jwt.CloudProgrammerJwtAuthenticationConverter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class AgentSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * 同时支持 Authorization Header 和 accessToken Cookie。
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {

        DefaultBearerTokenResolver defaultResolver =
                new DefaultBearerTokenResolver();

        return request -> {

            // 1. 优先从 Authorization 请求头获取
            String token = defaultResolver.resolve(request);

            if (StringUtils.hasText(token)) {
                return token;
            }

            // 2. 从 Cookie 中获取 JWT
            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {

                    if ("accessToken".equals(cookie.getName())
                            && StringUtils.hasText(cookie.getValue())) {

                        return cookie.getValue();
                    }
                }
            }

            return null;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CloudProgrammerJwtAuthenticationConverter jwtConverter,
            BearerTokenResolver bearerTokenResolver
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // 使用自定义 Token 解析器
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                );

        return http.build();
    }
}
