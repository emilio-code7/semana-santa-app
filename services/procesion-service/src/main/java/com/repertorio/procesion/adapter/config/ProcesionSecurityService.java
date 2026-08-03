package com.repertorio.procesion.adapter.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("procesionSecurity")
public class ProcesionSecurityService {

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
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(adminAuthority));
    }
}
