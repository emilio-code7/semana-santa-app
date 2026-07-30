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
        return hasRole(hermandadId, HermandadRole.HERMANDAD_ADMIN);
    }

    /**
     * CAPATAZ or HERMANDAD_ADMIN may manage titulares.
     */
    public boolean canManageTitulares(UUID hermandadId) {
        return hasAnyRole(hermandadId, HermandadRole.CAPATAZ, HermandadRole.HERMANDAD_ADMIN);
    }

    /**
     * Any member of the hermandad may read titulares.
     */
    public boolean isMember(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        // Fast path: check JWT authorities from hermandad_memberships claim
        var membershipPrefix = "HERMANDAD_" + hermandadId + "_";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith(membershipPrefix))) {
            return true;
        }

        // Fallback: check DB membership
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var userId = jwtAuth.getName();
            return hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId).isPresent();
        }

        return false;
    }

    private boolean hasRole(UUID hermandadId, HermandadRole role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        var requiredAuthority = "HERMANDAD_" + hermandadId + "_" + role.name();

        // Fast path: check JWT authorities from hermandad_memberships claim
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(requiredAuthority))) {
            return true;
        }

        // Fallback: check DB membership
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var userId = jwtAuth.getName();
            return hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId)
                    .map(m -> m.getRole() == role)
                    .orElse(false);
        }

        return false;
    }

    private boolean hasAnyRole(UUID hermandadId, HermandadRole... roles) {
        for (var role : roles) {
            if (hasRole(hermandadId, role)) return true;
        }
        return false;
    }
}
