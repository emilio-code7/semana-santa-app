package com.repertorio.marcha.application.service;

import com.repertorio.marcha.adapter.inbound.rest.dto.RunSheetResponse;
import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.CrucetaDefinedEvent;
import com.repertorio.marcha.domain.model.*;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import com.repertorio.marcha.domain.port.MarchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrucetaService {

    private final CrucetaRepository crucetaRepository;
    private final DomainEventPublisher eventPublisher;
    private final KnownProcesionRepository knownProcesionRepository;
    private final MarchaRepository marchaRepository;

    @Transactional
    public Cruceta defineCruceta(UUID pasoId, List<CrucetaItem> items) {
        if (!knownProcesionRepository.existsPasoById(pasoId)) {
            throw new PasoNotFoundException(pasoId);
        }
        for (var item : items) {
            if (!marchaRepository.existsById(item.getMarchaId())) {
                throw new MarchaNotFoundException(item.getMarchaId());
            }
            if (!knownProcesionRepository.existsRouteSectionById(item.getRouteSectionId())) {
                throw new IllegalArgumentException("Route section not found: " + item.getRouteSectionId());
            }
        }
        // Load existing aggregate and redefine in place — preserves aggregate ID and revision
        var cruceta = crucetaRepository.findByPasoId(pasoId)
                .map(existing -> {
                    existing.redefine(items);
                    return crucetaRepository.save(existing);
                })
                .orElseGet(() -> {
                    var newCruceta = new Cruceta(pasoId, items);
                    return crucetaRepository.save(newCruceta);
                });
        eventPublisher.publish(new CrucetaDefinedEvent(cruceta.getId(), pasoId, items.size()));
        return cruceta;
    }

    @Transactional(readOnly = true)
    public Cruceta getCruceta(UUID pasoId) {
        return crucetaRepository.findByPasoId(pasoId)
                .orElseThrow(() -> new CrucetaNotFoundException(pasoId));
    }

    @Transactional(readOnly = true)
    public RunSheetResponse getRunSheet(UUID procesionId, UUID pasoId) {
        var cruceta = crucetaRepository.findByPasoId(pasoId)
                .orElseThrow(() -> new CrucetaNotFoundException(pasoId));
        var routeSections = knownProcesionRepository.findRouteSectionsByProcesionId(procesionId);
        var itemsBySection = cruceta.getItems().stream()
                .collect(Collectors.groupingBy(CrucetaItem::getRouteSectionId));

        // Get or create progression for this paso
        var progression = crucetaRepository.findProgressionByPasoId(cruceta.getId(), pasoId)
                .orElse(null);

        var sortedSections = routeSections.stream()
                .sorted(Comparator.comparingInt(KnownRouteSection::getPosition))
                .toList();

        UUID currentSectionId = progression != null ? progression.getCurrentRouteSectionId() : null;
        UUID currentItemId = progression != null ? progression.getCurrentCrucetaItemId().orElse(null) : null;

        // Determine next section/item
        UUID nextSectionId = findNextSectionId(sortedSections, itemsBySection, currentSectionId, currentItemId);
        UUID nextItemId = findNextItemId(itemsBySection, currentSectionId, currentItemId);

        var sections = sortedSections.stream().map(section -> {
            var sectionItems = itemsBySection.getOrDefault(section.getId(), List.of()).stream()
                    .sorted(Comparator.comparingInt(CrucetaItem::getSequenceWithinSection))
                    .map(item -> new RunSheetResponse.RunSheetItem(
                            item.getId(),
                            item.getMarchaId(),
                            item.getSequenceWithinSection(),
                            item.getId().equals(currentItemId),
                            item.getId().equals(nextItemId),
                            item.getNotes()
                    ))
                    .toList();

            boolean isCurrent = section.getId().equals(currentSectionId);
            boolean isNext = section.getId().equals(nextSectionId);

            return new RunSheetResponse.RunSheetSection(
                    section.getId(),
                    section.getName(),
                    section.getPosition(),
                    isCurrent,
                    isNext,
                    sectionItems
            );
        }).toList();

        return new RunSheetResponse(pasoId, sections);
    }

    @Transactional
    public RunSheetResponse advanceCurrent(UUID procesionId, UUID pasoId, UUID routeSectionId, UUID crucetaItemId) {
        var cruceta = crucetaRepository.findByPasoId(pasoId)
                .orElseThrow(() -> new CrucetaNotFoundException(pasoId));

        // Validate routeSectionId belongs to this procesion
        var routeSections = knownProcesionRepository.findRouteSectionsByProcesionId(procesionId);
        if (routeSections.stream().noneMatch(rs -> rs.getId().equals(routeSectionId))) {
            throw new IllegalArgumentException("routeSectionId does not belong to this procesion");
        }

        // If crucetaItemId provided, validate it belongs to the requested section and cruceta
        if (crucetaItemId != null) {
            boolean itemExists = cruceta.getItems().stream()
                    .anyMatch(item -> item.getId().equals(crucetaItemId)
                            && item.getRouteSectionId().equals(routeSectionId));
            if (!itemExists) {
                throw new IllegalArgumentException("crucetaItemId does not belong to the requested section or cruceta");
            }
        }

        // Check if already at this position (idempotent)
        var existingProgression = crucetaRepository.findProgressionByPasoId(cruceta.getId(), pasoId);
        if (existingProgression.isPresent()) {
            var prog = existingProgression.get();
            if (prog.getCurrentRouteSectionId().equals(routeSectionId)
                    && prog.getCurrentCrucetaItemId().orElse(null) == crucetaItemId
                    && !(prog.getCurrentCrucetaItemId().isPresent() && crucetaItemId == null)) {
                // Same state — no-op; return current run-sheet
                return getRunSheet(procesionId, pasoId);
            }
            prog.advance(routeSectionId, crucetaItemId);
            crucetaRepository.saveProgression(prog);
        } else {
            var newProgression = new CrucetaProgression(cruceta.getId(), pasoId, routeSectionId);
            if (crucetaItemId != null) {
                newProgression.advance(routeSectionId, crucetaItemId);
            }
            crucetaRepository.saveProgression(newProgression);
        }

        return getRunSheet(procesionId, pasoId);
    }

    // Find the next section after the current position
    private UUID findNextSectionId(List<KnownRouteSection> sortedSections,
                                    Map<UUID, List<CrucetaItem>> itemsBySection,
                                    UUID currentSectionId, UUID currentItemId) {
        if (currentSectionId == null) {
            // No current progression — first section with items is next
            return sortedSections.stream()
                    .filter(s -> !itemsBySection.getOrDefault(s.getId(), List.of()).isEmpty())
                    .map(KnownRouteSection::getId)
                    .findFirst()
                    .orElse(null);
        }

        // If we're at a section with items and have a current item, find the next item/section
        if (currentItemId != null) {
            var currentSectionItems = itemsBySection.getOrDefault(currentSectionId, List.of());
            int currentIdx = -1;
            for (int i = 0; i < currentSectionItems.size(); i++) {
                if (currentSectionItems.get(i).getId().equals(currentItemId)) {
                    currentIdx = i;
                    break;
                }
            }
            if (currentIdx >= 0 && currentIdx < currentSectionItems.size() - 1) {
                // Next item is in same section — so next section is still this one
                return null; // null means "no next section" (next item is in current)
            }
        }

        // Move to next section
        boolean found = false;
        for (var section : sortedSections) {
            if (found) {
                if (!itemsBySection.getOrDefault(section.getId(), List.of()).isEmpty()) {
                    return section.getId();
                }
            }
            if (section.getId().equals(currentSectionId)) {
                found = true;
            }
        }
        return null;
    }

    // Find the next item after the current position
    private UUID findNextItemId(Map<UUID, List<CrucetaItem>> itemsBySection,
                                 UUID currentSectionId, UUID currentItemId) {
        if (currentSectionId == null || currentItemId == null) {
            return null;
        }

        var currentSectionItems = itemsBySection.getOrDefault(currentSectionId, List.of());
        int currentIdx = -1;
        for (int i = 0; i < currentSectionItems.size(); i++) {
            if (currentSectionItems.get(i).getId().equals(currentItemId)) {
                currentIdx = i;
                break;
            }
        }

        if (currentIdx >= 0 && currentIdx < currentSectionItems.size() - 1) {
            return currentSectionItems.get(currentIdx + 1).getId();
        }
        return null;
    }
}
