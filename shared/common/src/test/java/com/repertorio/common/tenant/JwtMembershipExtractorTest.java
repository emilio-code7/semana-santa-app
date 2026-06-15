package com.repertorio.common.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtMembershipExtractorTest {

    private final JwtMembershipExtractor extractor = new JwtMembershipExtractor();

    @Test
    void extractsValidMembershipsFromJwtClaim() {
        Jwt jwt = mock(Jwt.class);
        String json = """
                [
                    {"hermandadId":"h1","role":"HERMANDAD_ADMIN","pasoId":"p1"},
                    {"hermandadId":"h2","role":"HERMANO","pasoId":null}
                ]
                """;
        when(jwt.getClaimAsString("hermandad_memberships")).thenReturn(json);

        List<HermandadMembership> result = extractor.extract(jwt);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).hermandadId()).isEqualTo("h1");
        assertThat(result.get(0).role()).isEqualTo("HERMANDAD_ADMIN");
        assertThat(result.get(0).pasoId()).isEqualTo("p1");
        assertThat(result.get(1).hermandadId()).isEqualTo("h2");
        assertThat(result.get(1).role()).isEqualTo("HERMANO");
        assertThat(result.get(1).pasoId()).isNull();
    }

    @Test
    void returnsEmptyListWhenClaimIsAbsent() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("hermandad_memberships")).thenReturn(null);

        List<HermandadMembership> result = extractor.extract(jwt);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyListWhenClaimIsBlank() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("hermandad_memberships")).thenReturn("   ");

        List<HermandadMembership> result = extractor.extract(jwt);

        assertThat(result).isEmpty();
    }

    @Test
    void throwsOnMalformedClaimJson() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("hermandad_memberships")).thenReturn("not-valid-json");

        assertThatThrownBy(() -> extractor.extract(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed hermandad_memberships claim");
    }
}
