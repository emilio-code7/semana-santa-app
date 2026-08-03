package com.repertorio.marcha.adapter.inbound.rest;

import com.repertorio.marcha.adapter.inbound.rest.dto.ApiError;
import com.repertorio.marcha.domain.model.VersionMismatchException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleVersionMismatchReturns409() {
        var ex = new VersionMismatchException("Cruceta", UUID.randomUUID(), 1, 2);

        ResponseEntity<ApiError> response = handler.handleVersionMismatch(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertTrue(response.getBody().message().contains("version mismatch"));
    }

    @Test
    void handleDataIntegrityViolationReturns409() {
        var ex = new DataIntegrityViolationException(
                "null value in column \"cruceta_id\" violates not-null constraint");

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("null value in column \"cruceta_id\" violates not-null constraint",
                response.getBody().message());
    }

    @Test
    void handleOptimisticLockingFailureReturns409() {
        var ex = new ObjectOptimisticLockingFailureException("stale", new Object());

        ResponseEntity<ApiError> response = handler.handleOptimisticLockingFailure(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
    }
}
