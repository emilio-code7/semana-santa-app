package com.repertorio.common.tenant;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;

public class JwtMembershipExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<HermandadMembership> extract(Jwt jwt) {
        String claim = jwt.getClaimAsString("hermandad_memberships");
        if (claim == null || claim.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(claim, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Malformed hermandad_memberships claim: " + e.getMessage(), e);
        }
    }
}
