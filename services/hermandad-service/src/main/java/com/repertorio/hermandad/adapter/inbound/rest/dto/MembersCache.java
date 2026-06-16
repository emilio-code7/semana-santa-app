package com.repertorio.hermandad.adapter.inbound.rest.dto;

import com.repertorio.hermandad.domain.model.HermandadMember;

import java.util.List;

public record MembersCache(List<HermandadMember> members) {
}
