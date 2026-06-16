package com.repertorio.hermandad.adapter.inbound.rest;

import com.repertorio.hermandad.domain.model.HermandadNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HermandadNotFoundException.class)
    public ResponseEntity<String> handleHermandadNotFoundException(HermandadNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
