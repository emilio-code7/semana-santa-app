package com.repertorio.marcha.adapter.inbound.rest;

import com.repertorio.marcha.adapter.inbound.rest.dto.ApiError;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
import com.repertorio.marcha.domain.model.MarchaNotFoundException;
import com.repertorio.marcha.domain.model.ProcesionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MarchaNotFoundException.class)
    public ResponseEntity<ApiError> handleMarchaNotFound(MarchaNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(CrucetaNotFoundException.class)
    public ResponseEntity<ApiError> handleCrucetaNotFound(CrucetaNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ProcesionNotFoundException.class)
    public ResponseEntity<ApiError> handleProcesionNotFound(ProcesionNotFoundException ex) {
        var status = HttpStatus.NOT_FOUND;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        var status = HttpStatus.BAD_REQUEST;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        var status = HttpStatus.BAD_REQUEST;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    // Let Spring Security handle AccessDeniedException (returns 403 via ExceptionTranslationFilter)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        var status = HttpStatus.FORBIDDEN;
        var body = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage());
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
