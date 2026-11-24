package com.cloud.programmer.security.jwt;

import com.cloud.programmer.security.properties.JwtProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;

public class CloudProgrammerJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtProperties properties;

    private final JwtGrantedAuthoritiesConverter scopeConverter =
            new JwtGrantedAuthoritiesConverter();

    public CloudProgrammerJwtAuthenticationConverter(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // 保留 Spring Security 默认的 scope 权限解析
        Collection<GrantedAuthority> scopeAuthorities =
                scopeConverter.convert(jwt);

        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }

        // 解析自定义角色
        Object claim = jwt.getClaim(properties.getAuthoritiesClaim());

        if (claim instanceof Collection<?>) {
            for (Object value : (Collection<?>) claim) {
                addAuthority(authorities, value);
            }
        } else if (claim instanceof String) {
            String[] values = ((String) claim).split("[,\\s]+");

            for (String value : values) {
                addAuthority(authorities, value);
            }
        }

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                properties.getPrincipalClaimName()
        );
    }

    private void addAuthority(
            Collection<GrantedAuthority> authorities,
            Object value) {

        if (value == null) {
            return;
        }

        String authority = value.toString().trim();

        if (authority.isEmpty()) {
            return;
        }

        String prefix = properties.getAuthorityPrefix();

        if (prefix != null
                && !prefix.isEmpty()
                && !authority.startsWith(prefix)) {
            authority = prefix + authority;
        }

        authorities.add(new SimpleGrantedAuthority(authority));
    }
}