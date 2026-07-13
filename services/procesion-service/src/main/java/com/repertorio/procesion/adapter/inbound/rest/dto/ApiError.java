package com.repertorio.procesion.adapter.inbound.rest.dto;

public record ApiError(int status, String error, String message) {}
