package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.adapter.config.RedisConfig;
import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateHermandadRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.HermandadResponse;
import com.repertorio.hermandad.application.port.DomainEventPublisher;
import com.repertorio.hermandad.application.port.UserExistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.repertorio.hermandad.domain.event.HermandadCreatedEvent;
import com.repertorio.hermandad.domain.event.MemberAddedEvent;
import com.repertorio.hermandad.domain.event.MemberRemovedEvent;
import com.repertorio.hermandad.domain.event.MemberRoleChangedEvent;
import com.repertorio.hermandad.domain.model.*;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.repertorio.hermandad.adapter.config.RedisConfig.HERMANDAD_MEMBER;

@Service
@RequiredArgsConstructor
@Slf4j
public class HermandadService {

    private final HermandadRepository hermandadRepository;
    private final HermandadMemberRepository hermandadMemberRepository;

    private final DomainEventPublisher domainEventPublisher;
    private final UserExistencePort userExistencePort;

    @Transactional
    public HermandadResponse createHermandad(CreateHermandadRequest createHermandadRequest, String creatorUserId) {
        if (hermandadRepository.existsByName(createHermandadRequest.name())) {
            throw new HermandadAlreadyExistsException(createHermandadRequest.name());
        }
        Hermandad hermandad = new Hermandad(
                createHermandadRequest.name(),
                createHermandadRequest.city(),
                createHermandadRequest.foundedYear(),
                createHermandadRequest.description()
        );
        hermandad = hermandadRepository.save(hermandad);

        var adminMember = new HermandadMember(hermandad.getId(), creatorUserId, HermandadRole.HERMANDAD_ADMIN);
        hermandadMemberRepository.save(adminMember);

        HermandadCreatedEvent event = new HermandadCreatedEvent(
                hermandad.getId(),
                hermandad.getName(),
                hermandad.getCity(),
                hermandad.getFoundedYear()
        );
        domainEventPublisher.publish(event);

        return HermandadResponse.from(hermandad);
    }

    @Cacheable(RedisConfig.HERMANDAD)
    public HermandadResponse findHermandadById(UUID hermandadId) {
        Hermandad hermandad = hermandadRepository.findById(hermandadId)
                .orElseThrow(() -> new HermandadNotFoundException(hermandadId));
        log.info("hermandad {}", hermandad);
        return HermandadResponse.from(hermandad);
    }

    @CacheEvict(value = HERMANDAD_MEMBER, key = "#hermandadId")
    public HermandadMember addMember(UUID hermandadId, AddMemberRequest addMemberRequest) {
        if (!hermandadRepository.existsById(hermandadId)) {
            throw new HermandadNotFoundException(hermandadId);
        }
        if (!userExistencePort.exists(addMemberRequest.userId())) {
            throw new IllegalArgumentException("User does not exist in Keycloak: " + addMemberRequest.userId());
        }
        HermandadMember member = new HermandadMember(
                hermandadId,
                addMemberRequest.userId(),
                addMemberRequest.role()
        );
        member = hermandadMemberRepository.save(member);
        var memberAddedEvent = new MemberAddedEvent(member.getId(), member.getHermandadId(), member.getUserId(), member.getRole());
        domainEventPublisher.publish(memberAddedEvent);
        return member;
    }

    public Page<HermandadMember> getHermandadMembers(UUID hermandadId, Pageable pageable) {
        if (!hermandadRepository.existsById(hermandadId)) {
            throw new HermandadNotFoundException(hermandadId);
        }
        return hermandadMemberRepository.findByHermandadId(hermandadId, pageable);
    }

    public HermandadMember changeRole(UUID hermandadId, String userId, HermandadRole newRole) {
        HermandadMember member = hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId)
                .orElseThrow(() -> new HermandadMemberNotFoundException(hermandadId, userId));

        HermandadRole oldRole = member.getRole();
        member.changeRole(newRole);
        member = hermandadMemberRepository.save(member);

        var memberRoleChangedEvent = new MemberRoleChangedEvent(member.getId(), member.getHermandadId(), userId, oldRole, newRole);
        domainEventPublisher.publish(memberRoleChangedEvent);
        return member;
    }

    public void removeMember(UUID hermandadId, String userId) {
        HermandadMember member = hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId)
                .orElseThrow(() -> new HermandadMemberNotFoundException(hermandadId, userId));
        hermandadMemberRepository.delete(member);

        var memberRemovedEvent = new MemberRemovedEvent(member.getId(), member.getHermandadId(), userId, member.getRole());
        domainEventPublisher.publish(memberRemovedEvent);
    }

}
