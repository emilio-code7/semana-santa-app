package com.repertorio.hermandad.application.port;

import com.repertorio.hermandad.domain.model.HermandadRole;
import java.util.UUID;

public interface MembershipPort {
    void assignRole(String userId, UUID hermandadId, HermandadRole role);
}
