package com.example.ovikBot.OvikBot.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Lightweight, immutable security principal. Carries only what the
 * rest of the app needs without forcing a DB lookup on every request.
 */
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

    private UUID id;
    private String email;
    private String name;
    private String picture;
    private String role;
}