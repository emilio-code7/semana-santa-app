package com.repertorio.procesion.adapter.config;

import com.repertorio.procesion.domain.repository.ProcesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("procesionSecurity")
@RequiredArgsConstructor
public class ProcesionSecurityService {

    private final ProcesionRepository procesionRepository;

    public boolean isMember(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var membershipPrefix = "HERMANDAD_" + hermandadId + "_";
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith(membershipPrefix));
    }

    public boolean isAdmin(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var adminAuthority = "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN";
        var capatazAuthority = "HERMANDAD_" + hermandadId + "_CAPATAZ";
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(adminAuthority)
                        || a.getAuthority().equals(capatazAuthority));
    }

    public boolean canRead(UUID procesionId) {
        return procesionRepository.findById(procesionId)
                .map(p -> isMember(p.getHermandadId())).orElse(false);
    }

    public boolean canWrite(UUID procesionId) {
        return procesionRepository.findById(procesionId)
                .map(p -> isAdmin(p.getHermandadId())).orElse(false);
    }
}
