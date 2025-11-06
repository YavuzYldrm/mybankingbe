package com.mybanking.app.auth.dto;

import java.util.Set;

public record LoginResponse(String token, long expiresAt, Set<String> roles) {}
