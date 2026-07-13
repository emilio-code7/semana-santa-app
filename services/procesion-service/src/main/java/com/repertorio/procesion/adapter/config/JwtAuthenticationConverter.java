package com.repertorio.procesion.adapter.config;

import com.repertorio.common.tenant.HermandadMembership;
import com.repertorio.common.tenant.JwtMembershipExtractor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;

public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtMembershipExtractor extractor = new JwtMembershipExtractor();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var memberships = extractor.extract(jwt);
        var authorities = toAuthorities(memberships);
        var userId = jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, userId);
    }

    private Collection<GrantedAuthority> toAuthorities(List<HermandadMembership> memberships) {
        return memberships.stream()
                .map(m -> new SimpleGrantedAuthority(
                        "HERMANDAD_" + m.hermandadId() + "_" + m.role()))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
