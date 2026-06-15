package com.repertorio.hermandad.api.dto;

import com.repertorio.hermandad.domain.model.HermandadMember;

import java.util.List;

public record MembersCache(List<HermandadMember> members) {
}
