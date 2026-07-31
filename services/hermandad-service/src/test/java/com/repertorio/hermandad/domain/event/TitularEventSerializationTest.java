package com.repertorio.hermandad.domain.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TitularEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createdEventSerializesEventType() throws Exception {
        var event = new TitularCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "Jesus del Gran Poder", "description");

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("TITULAR_CREATED");
    }

    @Test
    void updatedEventSerializesEventType() throws Exception {
        var event = new TitularUpdatedEvent(UUID.randomUUID(), UUID.randomUUID(), "Jesus del Gran Poder", "description");

        var json = objectMapper.writeValueAsString(event);

        assertThat(objectMapper.readTree(json).get("eventType").asText()).isEqualTo("TITULAR_UPDATED");
    }
}
