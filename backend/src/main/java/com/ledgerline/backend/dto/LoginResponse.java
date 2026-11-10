package com.ledgerline.backend.dto;

public record LoginResponse(String token, Long userId, String username, String role, String status) {}