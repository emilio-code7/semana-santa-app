package com.repertorio.hermandad.adapter.inbound.rest;

import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
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

    @ExceptionHandler(HermandadMemberNotFoundException.class)
    public ResponseEntity<String> handleHermandadMemberNotFoundException(HermandadMemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
