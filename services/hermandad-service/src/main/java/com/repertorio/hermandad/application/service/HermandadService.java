package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.adapter.config.RedisConfig;
import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateHermandadRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.HermandadResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.MembersCache;
import com.repertorio.hermandad.application.port.EventPublisher;
import com.repertorio.hermandad.domain.event.MemberAddedEvent;
import com.repertorio.hermandad.domain.model.Hermandad;
import com.repertorio.hermandad.domain.model.HermandadCreatedEvent;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadNotFoundException;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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
    private final EventPublisher eventPublisher;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public HermandadResponse createHermandad(CreateHermandadRequest createHermandadRequest) {
        Hermandad hermandad = new Hermandad(
                createHermandadRequest.name(),
                createHermandadRequest.city(),
                createHermandadRequest.foundedYear()
        );
        hermandad = hermandadRepository.save(hermandad);

        HermandadCreatedEvent event = new HermandadCreatedEvent(
                hermandad.getId(),
                hermandad.getName(),
                hermandad.getCity(),
                hermandad.getFoundedYear()
        );

        eventPublisher.publish(
                "hermandad",
                hermandad.getId(),
                "HERMANDAD_CREATED",
                event
        );
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
        HermandadMember member = new HermandadMember(
                hermandadId,
                addMemberRequest.userId(),
                addMemberRequest.role()
        );
        member = hermandadMemberRepository.save(member);
        applicationEventPublisher.publishEvent(new MemberAddedEvent(member.getUserId(), member.getRole()));
        eventPublisher.publish(
                "hermandad-member",
                member.getId(),
                "MEMBER_ADDED",
                new MemberAddedEvent(member.getUserId(), member.getRole())
        );
        return member;
    }

    @Cacheable(value = HERMANDAD_MEMBER, key = "#hermandadId")
    public MembersCache getMembers(UUID hermandadId) {
        if (!hermandadRepository.existsById(hermandadId)) {
            throw new HermandadNotFoundException(hermandadId);
        }
        return new MembersCache(hermandadMemberRepository.findByHermandadId(hermandadId));
    }

}
