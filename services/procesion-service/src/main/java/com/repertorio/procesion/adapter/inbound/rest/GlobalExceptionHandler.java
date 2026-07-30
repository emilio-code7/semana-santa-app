package com.repertorio.procesion.adapter.inbound.rest;

import com.repertorio.procesion.adapter.inbound.rest.dto.ApiError;
import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProcesionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ProcesionNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        var status = HttpStatus.FORBIDDEN;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        var status = HttpStatus.CONFLICT;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        var status = HttpStatus.BAD_REQUEST;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        var status = HttpStatus.CONFLICT;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var status = HttpStatus.BAD_REQUEST;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        var body = new ApiError(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var body = new ApiError(status.value(), status.getReasonPhrase(), "An unexpected error occurred");
        return ResponseEntity.status(status).body(body);
    }
}
