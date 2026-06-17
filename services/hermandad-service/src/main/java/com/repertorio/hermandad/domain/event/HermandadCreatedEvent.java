package com.repertorio.hermandad.domain.model;

import java.util.UUID;

public record HermandadCreatedEvent(
        UUID id,
        String name,
        String city,
        Integer foundedYear
) {}