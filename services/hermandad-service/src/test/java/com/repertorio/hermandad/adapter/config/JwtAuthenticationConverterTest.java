package com.repertorio.hermandad.adapter.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    @Test
    void convertsMembershipToAuthority() {
        var jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("sub", "abc-123")
                .claim("hermandad_memberships", """
                        [{"hermandadId":"abc-123","role":"HERMANDAD_ADMIN"}]
                        """)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var auth = (JwtAuthenticationToken) converter.convert(jwt);

        assertEquals("abc-123", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("HERMANDAD_abc-123_HERMANDAD_ADMIN")));
    }
}
