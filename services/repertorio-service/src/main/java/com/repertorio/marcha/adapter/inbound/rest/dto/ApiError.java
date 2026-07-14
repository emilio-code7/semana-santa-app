package com.repertorio.marcha.adapter.inbound.rest.dto;

public record ApiError(int status, String error, String message) {}
