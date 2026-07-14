package com.repertorio.marcha.adapter.config.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("repertorioSecurity")
public class RepertorioSecurityService {

    public boolean isAdmin(UUID hermandadId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var adminAuthority = "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN";
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(adminAuthority));
    }
}
