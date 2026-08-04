package com.repertorio.marcha.application.event;

import com.repertorio.marcha.application.port.ProcessedEventStore;
import com.repertorio.marcha.domain.model.KnownPaso;
import com.repertorio.marcha.domain.model.KnownProcesion;
import com.repertorio.marcha.domain.model.KnownRouteSection;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesionEventProcessorTest {

    @Mock
    private KnownProcesionRepository knownProcesionRepository;

    @Mock
    private CrucetaRepository crucetaRepository;

    @Mock
    private ProcessedEventStore processedEventStore;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProcesionEventProcessor processor;

    @Captor
    private ArgumentCaptor<KnownProcesion> knownProcesionCaptor;

    @Captor
    private ArgumentCaptor<List<KnownPaso>> pasosCaptor;

    @Captor
    private ArgumentCaptor<List<KnownRouteSection>> routeSectionsCaptor;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void processesCreatedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\",\"eventId\":\"a0000000-0000-0000-0000-000000000001\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        processor.process(payload);

        verify(knownProcesionRepository).save(knownProcesionCaptor.capture());
        var saved = knownProcesionCaptor.getValue();
        assertThat(saved.getProcesionId()).hasToString("11111111-1111-1111-1111-111111111111");
        assertThat(saved.getHermandadId()).hasToString("22222222-2222-2222-2222-222222222222");
        assertThat(saved.getStatus()).isEqualTo("PLANNED");
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void processesStatusChangedEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\",\"eventId\":\"a0000000-0000-0000-0000-000000000002\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var hermandadId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var existing = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        when(knownProcesionRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(existing));

        processor.process(payload);

        assertThat(existing.getStatus()).isEqualTo("IN_PROGRESS");
        verify(knownProcesionRepository).save(existing);
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void skipsDuplicateEvent() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"eventId\":\"a0000000-0000-0000-0000-000000000003\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000003");
        when(processedEventStore.claim(eventId)).thenReturn(false);

        processor.process(payload);

        verify(objectMapper).readTree(payload);
        verify(processedEventStore).claim(eventId);
        verifyNoInteractions(knownProcesionRepository);
    }

    @Test
    void throwsOnMalformedJson() throws Exception {
        var payload = "not-valid-json";
        when(objectMapper.readTree(payload)).thenThrow(new RuntimeException("Invalid JSON"));

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process procesion event")
                .hasMessageContaining("Malformed event payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore, never()).claim(any());
    }

    @Test
    void throwsOnMissingId() throws Exception {
        var payload = "{\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"eventId\":\"a0000000-0000-0000-0000-000000000004\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000004");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnMissingHermandadIdForCreated() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"date\":\"2026-04-09\",\"eventId\":\"a0000000-0000-0000-0000-000000000005\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000005");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knownProcesionRepository);
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnStatusChangeForUnknownProcesion() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\",\"eventId\":\"a0000000-0000-0000-0000-000000000006\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000006");
        when(processedEventStore.claim(eventId)).thenReturn(true);
        when(knownProcesionRepository.findByProcesionId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnMissingNewStatusWhenFieldPresentButBlank() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":\"\",\"eventId\":\"a0000000-0000-0000-0000-000000000007\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000007");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void rejectsNewStatusNullValue() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":null,\"eventId\":\"a0000000-0000-0000-0000-000000000008\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000008");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void rejectsNonTextualNewStatus() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"newStatus\":123,\"eventId\":\"a0000000-0000-0000-0000-000000000009\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000009");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnMissingNewStatusInStatusPayload() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"previousStatus\":\"PLANNED\",\"eventId\":\"a0000000-0000-0000-0000-000000000010\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000010");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnMissingHermandadIdInStatusPayload() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"previousStatus\":\"PLANNED\",\"newStatus\":\"IN_PROGRESS\",\"eventId\":\"a0000000-0000-0000-0000-000000000011\",\"eventType\":\"PROCESION_STATUS_CHANGED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000011");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void deduplicatesWithUtf8Encoding() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"titulo\":\"Música\",\"eventId\":\"a0000000-0000-0000-0000-000000000012\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000012");
        when(processedEventStore.claim(eventId)).thenReturn(false);

        processor.process(payload);

        verify(objectMapper).readTree(payload);
        verify(processedEventStore).claim(eventId);
        verifyNoInteractions(knownProcesionRepository);
    }

    // --- PLAN FINALIZED ---

    @Test
    void processesPlanFinalizedEventForExistingKnownProcesion() throws Exception {
        var payload = "{\"procesionId\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-13\",\"time\":\"18:00:00\",\"planFinalizedAt\":\"2026-04-10T10:00:00Z\",\"pasos\":[{\"id\":\"33333333-3333-3333-3333-333333333333\",\"position\":0,\"titularId\":\"44444444-4444-4444-4444-444444444444\"}],\"routeSections\":[{\"id\":\"55555555-5555-5555-5555-555555555555\",\"name\":\"Salida\",\"position\":0,\"notes\":\"Salida notes\"}],\"eventId\":\"a0000000-0000-0000-0000-000000000013\",\"eventType\":\"PROCESION_PLAN_FINALIZED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000013");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var hermandadId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var existing = new KnownProcesion(procesionId, hermandadId, "PLANNED");
        when(knownProcesionRepository.findByProcesionId(procesionId)).thenReturn(Optional.of(existing));

        processor.process(payload);

        assertThat(existing.getDate()).isEqualTo(java.time.LocalDate.of(2026, 4, 13));
        assertThat(existing.getTime()).isEqualTo(java.time.LocalTime.of(18, 0));
        assertThat(existing.getPlanFinalizedAt()).isEqualTo(java.time.Instant.parse("2026-04-10T10:00:00Z"));
        verify(knownProcesionRepository).saveFullPlan(eq(existing), pasosCaptor.capture(), routeSectionsCaptor.capture());

        assertThat(pasosCaptor.getValue()).hasSize(1);
        var paso = pasosCaptor.getValue().get(0);
        assertThat(paso.getId()).hasToString("33333333-3333-3333-3333-333333333333");
        assertThat(paso.getProcesionId()).isEqualTo(procesionId);
        assertThat(paso.getPosition()).isZero();
        assertThat(paso.getTitularId()).hasToString("44444444-4444-4444-4444-444444444444");

        assertThat(routeSectionsCaptor.getValue()).hasSize(1);
        var section = routeSectionsCaptor.getValue().get(0);
        assertThat(section.getId()).hasToString("55555555-5555-5555-5555-555555555555");
        assertThat(section.getProcesionId()).isEqualTo(procesionId);
        assertThat(section.getName()).isEqualTo("Salida");
        assertThat(section.getPosition()).isZero();
        assertThat(section.getNotes()).isEqualTo("Salida notes");

        verify(processedEventStore).claim(eventId);
    }

    @Test
    void processesPlanFinalizedEventCreatesNewKnownProcesionIfMissing() throws Exception {
        var payload = "{\"procesionId\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-13\",\"time\":\"18:00:00\",\"planFinalizedAt\":\"2026-04-10T10:00:00Z\",\"pasos\":[{\"id\":\"33333333-3333-3333-3333-333333333333\",\"position\":0,\"titularId\":\"44444444-4444-4444-4444-444444444444\"}],\"routeSections\":[{\"id\":\"55555555-5555-5555-5555-555555555555\",\"name\":\"Salida\",\"position\":0,\"notes\":\"Salida notes\"}],\"eventId\":\"a0000000-0000-0000-0000-000000000014\",\"eventType\":\"PROCESION_PLAN_FINALIZED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000014");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(knownProcesionRepository.findByProcesionId(procesionId)).thenReturn(Optional.empty());

        processor.process(payload);

        verify(knownProcesionRepository).saveFullPlan(knownProcesionCaptor.capture(), pasosCaptor.capture(), routeSectionsCaptor.capture());
        var saved = knownProcesionCaptor.getValue();
        assertThat(saved.getProcesionId()).isEqualTo(procesionId);
        assertThat(saved.getStatus()).isEqualTo("PLANNED");
        assertThat(saved.getDate()).isEqualTo(java.time.LocalDate.of(2026, 4, 13));
        assertThat(saved.getPlanFinalizedAt()).isEqualTo(java.time.Instant.parse("2026-04-10T10:00:00Z"));

        assertThat(pasosCaptor.getValue()).hasSize(1);
        var paso = pasosCaptor.getValue().get(0);
        assertThat(paso.getId()).hasToString("33333333-3333-3333-3333-333333333333");
        assertThat(paso.getProcesionId()).isEqualTo(procesionId);
        assertThat(paso.getPosition()).isZero();
        assertThat(paso.getTitularId()).hasToString("44444444-4444-4444-4444-444444444444");

        assertThat(routeSectionsCaptor.getValue()).hasSize(1);
        var section = routeSectionsCaptor.getValue().get(0);
        assertThat(section.getId()).hasToString("55555555-5555-5555-5555-555555555555");
        assertThat(section.getProcesionId()).isEqualTo(procesionId);
        assertThat(section.getName()).isEqualTo("Salida");
        assertThat(section.getPosition()).isZero();
        assertThat(section.getNotes()).isEqualTo("Salida notes");

        verify(processedEventStore).claim(eventId);
    }

    @Test
    void rejectsPlanFinalizedEventWithMissingDate() throws Exception {
        var payload = "{\"procesionId\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"time\":\"18:00:00\",\"planFinalizedAt\":\"2026-04-10T10:00:00Z\",\"eventId\":\"a0000000-0000-0000-0000-000000000015\",\"eventType\":\"PROCESION_PLAN_FINALIZED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000015");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class);

        verify(knownProcesionRepository, never()).saveFullPlan(any(), anyList(), anyList());
        verify(processedEventStore).claim(eventId);
    }

    // --- EVENT-ID DEDUP SEMANTICS (ticket 13) ---

    @Test
    void twoEventsWithSamePayloadButDifferentEventIdsAreBothProcessed() throws Exception {
        var payload1 = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\",\"eventId\":\"a0000000-0000-0000-0000-000000000016\",\"eventType\":\"PROCESION_CREATED\"}";
        var payload2 = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"time\":\"18:00:00\",\"eventId\":\"a0000000-0000-0000-0000-000000000017\",\"eventType\":\"PROCESION_CREATED\"}";
        var node1 = realMapper.readTree(payload1);
        var node2 = realMapper.readTree(payload2);
        when(objectMapper.readTree(payload1)).thenReturn(node1);
        when(objectMapper.readTree(payload2)).thenReturn(node2);
        UUID eventId1 = UUID.fromString("a0000000-0000-0000-0000-000000000016");
        UUID eventId2 = UUID.fromString("a0000000-0000-0000-0000-000000000017");
        when(processedEventStore.claim(eventId1)).thenReturn(true);
        when(processedEventStore.claim(eventId2)).thenReturn(true);

        processor.process(payload1);
        processor.process(payload2);

        verify(knownProcesionRepository, times(2)).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId1);
        verify(processedEventStore).claim(eventId2);
    }

    @Test
    void sameEventIdDeliveredTwiceSkipsSecond() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"date\":\"2026-04-09\",\"eventId\":\"a0000000-0000-0000-0000-000000000018\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000018");
        when(processedEventStore.claim(eventId)).thenReturn(true, false);

        processor.process(payload);
        processor.process(payload);

        verify(knownProcesionRepository, times(1)).save(any(KnownProcesion.class));
        verify(processedEventStore, times(2)).claim(eventId);
    }

    // --- PROCEESION_DELETED (ticket 13) ---

    @Test
    void processesDeletedEventRemovesPlanProjections() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"eventId\":\"a0000000-0000-0000-0000-000000000019\",\"eventType\":\"PROCESION_DELETED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000019");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var paso = new KnownPaso(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                procesionId, 0, UUID.fromString("44444444-4444-4444-4444-444444444444"));
        when(knownProcesionRepository.findPasosByProcesionId(procesionId)).thenReturn(List.of(paso));

        processor.process(payload);

        verify(crucetaRepository).deleteByPasoId(paso.getId());
        verify(knownProcesionRepository).deleteByProcesionId(procesionId);
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void deletesIdempotentlyWhenProcesionUnknown() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"eventId\":\"a0000000-0000-0000-0000-000000000021\",\"eventType\":\"PROCESION_DELETED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000021");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        var procesionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(knownProcesionRepository.findPasosByProcesionId(procesionId)).thenReturn(List.of());

        processor.process(payload);

        verify(knownProcesionRepository).deleteByProcesionId(procesionId);
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void unknownEventTypeThrows() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"eventId\":\"a0000000-0000-0000-0000-000000000020\",\"eventType\":\"PROCESION_WHATEVER\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000020");
        when(processedEventStore.claim(eventId)).thenReturn(true);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown procesion event type");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore).claim(eventId);
    }

    @Test
    void throwsOnMissingEventId() throws Exception {
        var payload = "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"hermandadId\":\"22222222-2222-2222-2222-222222222222\",\"eventType\":\"PROCESION_CREATED\"}";
        var node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);

        assertThatThrownBy(() -> processor.process(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");

        verify(knownProcesionRepository, never()).save(any(KnownProcesion.class));
        verify(processedEventStore, never()).claim(any());
    }
}
