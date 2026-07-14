package com.example.ovikBot.OvikBot.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String name,
        String email,
        String picture,
        String role
) {
}