package com.repertorio.hermandad.adapter.inbound.rest;

import com.repertorio.hermandad.adapter.inbound.rest.dto.ApiError;
import com.repertorio.hermandad.domain.model.HermandadAlreadyExistsException;
import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import com.repertorio.hermandad.domain.model.HermandadNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HermandadNotFoundException.class)
    public ResponseEntity<ApiError> handleHermandadNotFoundException(HermandadNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(HermandadAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleHermandadAlreadyExists(HermandadAlreadyExistsException ex) {
        var status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(HermandadMemberNotFoundException.class)
    public ResponseEntity<ApiError> handleHermandadMemberNotFoundException(HermandadMemberNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        var status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        var status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var status = HttpStatus.BAD_REQUEST;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        var status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), "An unexpected error occurred"));
    }
}
