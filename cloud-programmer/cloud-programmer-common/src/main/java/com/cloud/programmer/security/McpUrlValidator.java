package com.cloud.programmer.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * MCP Server URL 安全校验
 */
public final class McpUrlValidator {

    private McpUrlValidator() {
    }

    /**
     * 校验 MCP URL。
     *
     * trustedHosts 用于显式信任的内部 MCP 主机，
     * 例如 Docker 内部服务地址。
     */
    public static void validate(String rawUrl, Collection<String> trustedHosts) {

        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("MCP Server URL 不能为空");
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                throw new IllegalArgumentException("MCP URL 格式不正确");
            }
            scheme = scheme.toLowerCase();
            host = normalizeHost(host);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("MCP URL 仅支持 HTTP/HTTPS");
            }
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("MCP URL 不允许包含用户凭据");
            }
            if (uri.getPort() < -1 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("MCP URL 端口不合法");
            }
            Set<String> trusted = normalizeTrustedHosts(trustedHosts);
            boolean trustedHost = trusted.contains(host);
            // 非显式信任的公网服务必须使用 HTTPS
            if (!trustedHost && !"https".equals(scheme)) {
                throw new IllegalArgumentException(
                        "公网 MCP Server 必须使用 HTTPS");
            }

            if (trustedHost) {
                return;
            }

            // 禁止 localhost 等本机域名
            if (isLocalHostname(host)) {
                throw new IllegalArgumentException(
                        "不允许连接本机或本地域名");
            }

            // DNS 解析失败时拒绝连接
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException(
                        "MCP Server 域名无法解析");
            }

            // 一个域名只要解析出一个非公网 IP，就拒绝
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    throw new IllegalArgumentException(
                            "MCP Server 不允许指向内网或保留 IP");
                }
            }

        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "MCP Server 域名解析失败", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "MCP Server URL 校验失败", e);
        }
    }

    private static String normalizeHost(String host) {
        String value = host.toLowerCase().trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static Set<String> normalizeTrustedHosts(Collection<String> trustedHosts) {
        if (trustedHosts == null || trustedHosts.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String host : trustedHosts) {
            if (host != null && !host.trim().isEmpty()) {
                result.add(normalizeHost(host));
            }
        }
        return result;
    }

    private static boolean isLocalHostname(String host) {
        return "localhost".equals(host)
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal");
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        // IPv6 Unique Local Address：fc00::/7
        if (bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC) {
            return false;
        }
        // IPv4 特殊地址段
        if (bytes.length == 4) {
            int a = bytes[0] & 0xFF;
            int b = bytes[1] & 0xFF;
            // 0.0.0.0/8
            if (a == 0) {
                return false;
            }
            // 100.64.0.0/10 运营商级 NAT
            if (a == 100 && b >= 64 && b <= 127) {
                return false;
            }
            // 192.0.0.0/24
            if (a == 192 && b == 0) {
                return false;
            }
            // 198.18.0.0/15 基准测试网段
            if (a == 198 && (b == 18 || b == 19)) {
                return false;
            }
            // 240.0.0.0/4 保留地址
            if (a >= 240) {
                return false;
            }
        }
        return true;
    }
}
