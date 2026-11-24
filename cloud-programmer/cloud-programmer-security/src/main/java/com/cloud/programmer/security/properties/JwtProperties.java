package com.cloud.programmer.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
@Data
public class JwtProperties {

    /**
     * JWT 签发者
     */
    private String issuer;

    /**
     * RSA 公钥位置
     */
    private String publicKeyLocation;

    /**
     * RSA 私钥位置
     * Agent 不需要配置
     */
    private String privateKeyLocation;

    /**
     * JWT 中存放角色或权限的 Claim 名称
     */
    private String authoritiesClaim = "roles";

    /**
     * 权限前缀
     */
    private String authorityPrefix = "ROLE_";

    /**
     * Spring Security Authentication 中使用的主体 Claim
     */
    private String principalClaimName = "sub";

}