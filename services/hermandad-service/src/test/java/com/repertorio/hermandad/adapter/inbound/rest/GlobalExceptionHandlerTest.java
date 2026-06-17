package com.repertorio.hermandad.adapter.inbound.rest;

import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import com.repertorio.hermandad.domain.model.HermandadNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void hermandadNotFoundReturns404() {
        var ex = new HermandadNotFoundException(UUID.randomUUID());

        var response = handler.handleHermandadNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("not found");
    }

    @Test
    void hermandadMemberNotFoundReturns404() {
        var ex = new HermandadMemberNotFoundException(UUID.randomUUID(), "user-1");

        var response = handler.handleHermandadMemberNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("not found");
    }

    @Test
    void illegalArgumentReturns400() {
        var ex = new IllegalArgumentException("bad argument");

        var response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad argument");
    }

    @Test
    void dataIntegrityViolationReturns409() {
        var ex = new DataIntegrityViolationException("duplicate key");

        var response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("duplicate");
    }

    @Test
    void methodArgumentNotValidReturns400WithFieldErrors() {
        var ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("obj", "name", "must not be blank")
        ));

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("name", "must not be blank");
    }

    @Test
    void genericExceptionReturns500() {
        var ex = new RuntimeException("unexpected error");

        var response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred");
    }
}
