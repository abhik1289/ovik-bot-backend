package com.example.ovikBot.OvikBot.dto;

import java.util.List;
import java.util.Map;

/**
 * Body of {@code PUT /api/chats/{id}}.
 *
 * <p>
 * The frontend sends the full conversation each time so the backend can
 * stay stateless about streaming. Anything not in the body is overwritten.
 * </p>
 */
public record ChatHistoryUpsertRequest(
        String title,
        String preview,
        String mode,
        Boolean pinned,
        List<Map<String, Object>> messages) {
}