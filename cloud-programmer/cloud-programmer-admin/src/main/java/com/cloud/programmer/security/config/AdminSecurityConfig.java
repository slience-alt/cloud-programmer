package com.cloud.programmer.security.config;

import com.cloud.programmer.security.filter.InternalTokenFilter;
import com.cloud.programmer.security.jwt.CloudProgrammerJwtAuthenticationConverter;
import com.cloud.programmer.security.properties.InternalTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final InternalTokenProperties internalTokenProperties;

    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * 第一条过滤器链：只处理内部服务接口
     *
     * 不配置 oauth2ResourceServer，因此不会进行 JWT 校验。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http) throws Exception {

        InternalTokenFilter internalTokenFilter =
                new InternalTokenFilter(
                        internalTokenProperties.getToken()
                );

        http
                .securityMatcher("/internal/**")
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("INTERNAL_SERVICE")
                )
                .addFilterBefore(
                        internalTokenFilter,
                        AuthorizationFilter.class
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                );

        return http.build();
    }

    /**
     * 第二条过滤器链：处理普通业务接口
     *
     * 使用 JWT 认证。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http, CloudProgrammerJwtAuthenticationConverter jwtConverter) throws Exception {
        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
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