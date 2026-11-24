package com.cloud.programmer.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前 Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前 JWT
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = getAuthentication();

        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getToken();
        }

        return null;
    }

    /**
     * 根据 Claim 名称获取当前用户信息
     */
    public static Object getClaim(String claimName) {
        Jwt jwt = getCurrentJwt();

        if (jwt == null) {
            return null;
        }

        return jwt.getClaim(claimName);
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getCurrentUserId() {
        Object userId = getClaim("userId");

        if (userId != null) {
            return Long.valueOf(String.valueOf(userId));
        }

        Jwt jwt = getCurrentJwt();

        return jwt == null ? null : Long.valueOf(jwt.getSubject());
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Object username = getClaim("username");

        if (username != null) {
            return String.valueOf(username);
        }

        Jwt jwt = getCurrentJwt();

        return jwt == null ? null : jwt.getSubject();
    }

    /**
     * 获取当前用户权限
     */
    public static Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication authentication = getAuthentication();

        if (authentication == null) {
            return Collections.emptyList();
        }

        return authentication.getAuthorities();
    }

    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasAuthority(String authority) {
        return getAuthorities()
                .stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        String authority = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return hasAuthority(authority);
    }

    /**
     * 判断当前是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }
}