package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.MarchaAddedEvent;
import com.repertorio.marcha.domain.event.MarchaRemovedEvent;
import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.marcha.domain.model.Marcha;
import com.repertorio.marcha.domain.model.MarchaNotFoundException;
import com.repertorio.marcha.domain.port.MarchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarchaService {

    private final MarchaRepository marchaRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public Marcha createMarcha(String title, String composer, BandType bandType,
                               int durationSeconds, Integer compositionYear, String youtubeUrl) {
        var marcha = Marcha.create(title, composer, bandType, durationSeconds, compositionYear, youtubeUrl);
        marcha = marchaRepository.save(marcha);
        eventPublisher.publish(new MarchaAddedEvent(marcha.getId(), title, composer, bandType, compositionYear, youtubeUrl));
        return marcha;
    }

    @Transactional(readOnly = true)
    public Optional<Marcha> getMarcha(UUID id) {
        return marchaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Marcha> listMarchas() {
        return marchaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Marcha> search(String query) {
        return marchaRepository.findByTitleContainingIgnoreCaseOrComposerContainingIgnoreCase(query, query);
    }

    @Transactional
    public Marcha updateMarcha(UUID id, String title, String composer, BandType bandType,
                               int durationSeconds, Integer compositionYear, String youtubeUrl) {
        var marcha = marchaRepository.findById(id)
                .orElseThrow(() -> new MarchaNotFoundException(id));
        marcha.update(title, composer, bandType, durationSeconds, compositionYear, youtubeUrl);
        return marchaRepository.save(marcha);
    }

    @Transactional
    public void deleteMarcha(UUID id) {
        var marcha = marchaRepository.findById(id)
                .orElseThrow(() -> new MarchaNotFoundException(id));
        marchaRepository.deleteById(id);
        eventPublisher.publish(new MarchaRemovedEvent(id, marcha.getTitle()));
    }
}
