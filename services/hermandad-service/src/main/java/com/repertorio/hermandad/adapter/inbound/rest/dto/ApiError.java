package com.repertorio.hermandad.adapter.inbound.rest.dto;

public record ApiError(int status, String error, String message) {}
