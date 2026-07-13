package com.repertorio.procesion.adapter.inbound.rest;

import com.repertorio.procesion.adapter.inbound.rest.dto.ApiError;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404WithApiError() {
        var id = UUID.randomUUID();
        var ex = new ProcesionNotFoundException(id);

        ResponseEntity<ApiError> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertTrue(response.getBody().message().contains(id.toString()));
    }

    @Test
    void handleIllegalArgumentReturns400WithApiError() {
        var ex = new IllegalArgumentException("Bad stuff");

        ResponseEntity<ApiError> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Bad stuff", response.getBody().message());
    }

    @Test
    void handleGenericReturns500WithApiError() {
        var ex = new RuntimeException("Kaboom");

        ResponseEntity<ApiError> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
