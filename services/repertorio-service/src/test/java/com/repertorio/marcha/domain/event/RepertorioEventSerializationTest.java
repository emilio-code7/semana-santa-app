package com.repertorio.marcha.domain.event;

import com.repertorio.marcha.domain.model.BandType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The outbox payload is the full JSON serialization of the event record. The eventType
 * discriminator must be a serialized record component (envelope contract section 4);
 * consumers dispatch on the payload's "eventType" field (ticket 13).
 */
class RepertorioEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void marchaAddedEventSerializesEventType() throws Exception {
        var event = new MarchaAddedEvent(
                UUID.randomUUID(), "Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO, 1919, null);

        var json = objectMapper.writeValueAsString(event);
        var tree = objectMapper.readTree(json);

        assertThat(tree.get("eventType").asText()).isEqualTo("MARCHA_ADDED");
        assertThat(tree.get("eventId")).isNotNull();
        assertThat(tree.get("occurredAt")).isNotNull();
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void marchaRemovedEventSerializesEventType() throws Exception {
        var event = new MarchaRemovedEvent(UUID.randomUUID(), "Amarguras");

        var json = objectMapper.writeValueAsString(event);
        var tree = objectMapper.readTree(json);

        assertThat(tree.get("eventType").asText()).isEqualTo("MARCHA_REMOVED");
        assertThat(tree.get("eventId")).isNotNull();
        assertThat(tree.get("occurredAt")).isNotNull();
        assertThat(event.schemaVersion()).isEqualTo(1);
    }

    @Test
    void crucetaDefinedEventSerializesEventType() throws Exception {
        var event = new CrucetaDefinedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3);

        var json = objectMapper.writeValueAsString(event);
        var tree = objectMapper.readTree(json);

        assertThat(tree.get("eventType").asText()).isEqualTo("CRUCETA_DEFINED");
        assertThat(tree.get("crucetaId")).isNotNull();
        assertThat(tree.get("procesionId")).isNotNull();
        assertThat(tree.get("pasoId")).isNotNull();
        assertThat(tree.get("itemCount")).isNotNull();
        assertThat(event.schemaVersion()).isEqualTo(1);
    }
}
