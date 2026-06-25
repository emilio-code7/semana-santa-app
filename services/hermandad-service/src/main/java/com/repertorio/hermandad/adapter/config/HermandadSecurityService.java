package com.repertorio.hermandad.adapter.config;

import com.repertorio.hermandad.domain.model.HermandadRole;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("hermandadSecurity")
@RequiredArgsConstructor
public class HermandadSecurityService {

    private final HermandadMemberRepository hermandadMemberRepository;

    public boolean isAdmin(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        var adminAuthority = "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN";

        // Fast path: check JWT authorities from hermandad_memberships claim
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(adminAuthority))) {
            return true;
        }

        // Fallback: check DB membership
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var userId = jwtAuth.getName();
            return hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId)
                    .map(m -> m.getRole() == HermandadRole.HERMANDAD_ADMIN)
                    .orElse(false);
        }

        return false;
    }
}
