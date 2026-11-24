package com.cloud.programmer.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final String internalToken;

    public InternalTokenFilter(String internalToken) {
        if (internalToken == null || internalToken.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "内部服务 Token 未配置"
            );
        }

        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestToken = request.getHeader(TOKEN_HEADER);
        if (requestToken == null || !MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), requestToken.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal token");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "agent-runtime",
                        null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority(
                                        "ROLE_INTERNAL_SERVICE"
                                )
                        )
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }
}