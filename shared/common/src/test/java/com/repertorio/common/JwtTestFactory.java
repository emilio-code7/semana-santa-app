package com.repertorio.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.repertorio.common.tenant.HermandadMembership;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

public class JwtTestFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Jwt withMemberships(List<HermandadMembership> memberships) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(memberships);
            return Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .claim("hermandad_memberships", json)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize memberships", e);
        }
    }

    public static Jwt empty() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
