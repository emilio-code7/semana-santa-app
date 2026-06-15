package com.repertorio.hermandad.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repertorio.hermandad.api.dto.AddMemberRequest;
import com.repertorio.hermandad.api.dto.CreateHermandadRequest;
import com.repertorio.hermandad.api.dto.HermandadResponse;
import com.repertorio.hermandad.api.dto.MembersCache;
import com.repertorio.hermandad.application.event.MemberAddedEvent;
import com.repertorio.hermandad.config.RedisConfig;
import com.repertorio.hermandad.domain.model.*;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import com.repertorio.hermandad.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.repertorio.hermandad.config.RedisConfig.HERMANDAD_MEMBER;

@Service
@RequiredArgsConstructor
@Slf4j
public class HermandadService {

    private final HermandadRepository hermandadRepository;
    private final HermandadMemberRepository hermandadMemberRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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

        OutboxEvent outboxEvent = new OutboxEvent(
                "hermandad",
                hermandad.getId(),
                "HERMANDAD_CREATED",
                toJson(event)
        );
        outboxEventRepository.save(outboxEvent);
        return HermandadResponse.from(hermandad);
    }

    @Cacheable(RedisConfig.HERMANDAD)
    public HermandadResponse findHermandadById(UUID hermandadId) {
        Hermandad hermandad = hermandadRepository.findById(hermandadId)
                .orElseThrow(() -> new HermandadNotFoundException(hermandadId));
        log.info("hermandad {}", hermandad);
        return HermandadResponse.from(hermandad);
    }

    @CacheEvict(value = RedisConfig.HERMANDAD_MEMBER, key = "#hermandadId")
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
        return member;
    }

    @Cacheable(value = HERMANDAD_MEMBER, key = "#hermandadId")
    public MembersCache getMembers(UUID hermandadId) {
        if (!hermandadRepository.existsById(hermandadId)) {
            throw new HermandadNotFoundException(hermandadId);
        }
        return new MembersCache(hermandadMemberRepository.findByHermandadId(hermandadId));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
